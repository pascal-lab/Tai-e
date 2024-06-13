/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.pta.plugin.android.icc;

import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.android.info.TransferDataInfo;
import pascal.taie.android.info.TransferFilterInfo;
import pascal.taie.android.info.UriData;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.NullLiteral;
import pascal.taie.ir.exp.StringLiteral;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Subsignature;
import pascal.taie.language.type.ClassType;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static pascal.taie.android.util.IntentInfoMatcher.normalizeMimeType;
import static pascal.taie.android.util.IntentInfoMatcher.normalizeScheme;

/**
 * Handles send and reply icc.
 */
public class SendAndReplyICCHandler extends ICCHandler {

    private static final String DEFAULT_CATEGORY = "android.intent.category.DEFAULT";

    private static final Subsignature ON_NEW_INTENT_SUB_SIG = Subsignature.get("void onNewIntent(android.content.Intent)");

    /**
     * Sets from the ICCInfo has been processed.
     */
    private final Set<ICCInfo> processedICCInfos = Sets.newSet();

    public SendAndReplyICCHandler(ICCContext context) {
        super(context);
    }

    @Override
    public void onPhaseFinish() {
        // process source intent to target intent
        handlerContext.sourceComponent2ICCInfo().forEach((sourceComponent, sourceICCInfo) -> {
            if (!processedICCInfos.contains(sourceICCInfo)) {
                boolean isStartActivity = sourceICCInfo.kind() == ICCInfoKind.START_ACTIVITY || sourceICCInfo.kind() == ICCInfoKind.START_ACTIVITY_FOR_RESULT;
                solver.getPointsToSetOf(sourceICCInfo.info()).forEach(csObj -> {
                    Set<JClass> targetComponents = getTargetComponents(csObj, isStartActivity);
                    addICCGraph(transferVarToClass(sourceComponent), targetComponents);
                    targetComponents.forEach(targetComponent -> processICC(targetComponent, sourceICCInfo));
                });
            }
        });

        // process reply intent
        processActivityReplyIntent();
        processHandlerSendMsgOrMessengerReplyMsg();
        // clear up the processed icc info
        clearICCInfos();
    }

    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        if (edge instanceof ICCCallEdge iccCallEdge) {
            // pass argument
            ICCInfo sourceICCInfo = iccCallEdge.getICCInfo();
            CSVar source = sourceICCInfo.info();
            CSMethod csCallee = edge.getCallee();
            Context calleeCtx = csCallee.getContext();
            JMethod callee = csCallee.getMethod();
            callee.getIR().getParams().forEach(param -> {
                if (param.getType().equals(source.getType())) {
                    solver.addPFGEdge(new ICCEdge(source, csManager.getCSVar(calleeCtx, param)), param.getType());
                }
            });
            processedICCInfos.add(sourceICCInfo);
        }
    }

    private void addICCGraph(Set<JClass> sourceComponents, Set<JClass> targetComponents) {
        sourceComponents.forEach(sourceComponent ->
                targetComponents.forEach(targetComponent ->
                        handlerContext.componentICCGraph().put(sourceComponent, targetComponent)
                )
        );
    }

    private void processICC(JClass targetComponent, ICCInfo sourceICCInfo) {
        if (sourceICCInfo.kind().equals(ICCInfoKind.BIND_SERVICE)) {
            processComplexICC(targetComponent, sourceICCInfo);
        } else {
            processCommonICC(targetComponent, sourceICCInfo);
        }
    }

    private void processCommonICC(JClass targetComponent, ICCInfo sourceICCInfo) {
        handlerContext.targetComponent2ICCInfo().forEach((component, targetICCInfo) -> {
            if (sourceICCInfo.kind().equals(targetICCInfo.kind())
                    && transferVarToClass(component).contains(targetComponent)) {
                switch (sourceICCInfo.kind()) {
                    case START_ACTIVITY, START_ACTIVITY_FOR_RESULT -> {
                        processedICCInfos.add(sourceICCInfo);
                        solver.addPFGEdge(new ICCEdge(sourceICCInfo.info(), targetICCInfo.info()), targetICCInfo.info().getType());

                        JMethod callee = handlerContext.lifecycleHelper().getLifeCycleMethod(targetComponent, ON_NEW_INTENT_SUB_SIG);
                        if (callee != null) {
                            addICCCallEdge(targetComponent, sourceICCInfo, callee);
                        }
                    }
                    case SEND_BROADCAST, START_SERVICE ->
                            addICCCallEdge(targetComponent, sourceICCInfo, targetICCInfo.info().getVar().getMethod());
                }
            }
        });
    }

    private void processComplexICC(JClass targetComponent, ICCInfo sourceICCInfo) {
        sendMessage(transferSendMsg(sourceICCInfo), transferHandlerMsg(targetComponent));
        processOther(targetComponent, sourceICCInfo);
    }

    private Set<ICCInfo> transferSendMsg(ICCInfo sourceICCInfo) {
        // intent -> iBinder -> messenger -> send message
        return handlerContext.intent2IBinder().get(sourceICCInfo.info()).stream()
                .flatMap(iBinder -> solver.getPointsToSetOf(iBinder).objects())
                .flatMap(iBinder ->
                        handlerContext.messenger2IBinder()
                                .entrySet()
                                .stream()
                                .filter(entry -> solver.getPointsToSetOf(entry.getValue()).contains(iBinder))
                                .map(entry -> handlerContext.sendMessage().get(entry.getKey()))
                )
                .flatMap(Set::stream)
                .filter(sourceMsg -> !processedICCInfos.contains(sourceMsg))
                .collect(Collectors.toSet());
    }

    private Set<ICCInfo> transferHandlerMsg(JClass targetComponent) {
        // target component -> messenger -> handle message
        return handlerContext.serviceComponent2Messenger().entrySet().stream()
                .filter(entry -> transferVarToClass(entry.getKey()).contains(targetComponent))
                .flatMap(entry -> solver.getPointsToSetOf(entry.getValue()).objects())
                .flatMap(csObj -> handlerContext.handleMessage().get(csObj).stream())
                .collect(Collectors.toSet());
    }

    private void processOther(JClass targetComponent, ICCInfo sourceICCInfo) {
        // intent -> iBinder -> aidl
        Set<CSVar> aidlCSVars = handlerContext.intent2IBinder().get(sourceICCInfo.info()).stream()
                .flatMap(iBinder -> solver.getPointsToSetOf(iBinder).objects())
                .flatMap(iBinder ->
                        handlerContext.iBinder2Aidl()
                                .entrySet()
                                .stream()
                                .filter(entry -> solver.getPointsToSetOf(entry.getKey()).contains(iBinder))
                                .map(Map.Entry::getValue)
                ).collect(Collectors.toSet());

        Set<CSVar> iBinderVars = handlerContext.intent2IBinder().get(sourceICCInfo.info());

        // target component -> iBinder -> iBinderObj
        Set<CSObj> iBinderCSObjs = handlerContext.serviceComponent2IBinder().entrySet().stream()
                .filter(entry -> transferVarToClass(entry.getKey()).contains(targetComponent))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());

        aidlCSVars.forEach(aidl -> iBinderCSObjs.forEach(csObj -> solver.addPointsTo(aidl, csObj)));
        iBinderVars.forEach(iBinder -> iBinderCSObjs.forEach(csObj -> solver.addPointsTo(iBinder, csObj)));
        if ((!aidlCSVars.isEmpty() || !iBinderVars.isEmpty()) && !iBinderCSObjs.isEmpty()) {
            processedICCInfos.add(sourceICCInfo);
        }
    }


    private void sendMessage(Set<ICCInfo> sources, Set<ICCInfo> targets) {
        sources.forEach(source ->
                targets.forEach(target -> sendMessage(source, target)));
    }

    private void processActivityReplyIntent() {
        // targetComponent is the sourceComponent of the reply intent
        handlerContext.targetComponent2ICCInfo().forEach((targetVar, sourceICCInfo) -> {
            if (!processedICCInfos.contains(sourceICCInfo)
                    && sourceICCInfo.kind().equals(ICCInfoKind.START_ACTIVITY_FOR_RESULT_REPLY)) {
                Set<JClass> replyTargetComponents = getSourceComponentByTargetComponent(transferVarToClass(targetVar));
                // sourceComponent is the targetComponent of the reply intent
                replyTargetComponents.forEach(replyTargetComponent ->
                        handlerContext.sourceComponent2ICCInfo().forEach((component, targetICCInfo) -> {
                            if (targetICCInfo.kind().equals(ICCInfoKind.START_ACTIVITY_FOR_RESULT_REPLY)
                                    && transferVarToClass(component).contains(replyTargetComponent)) {
                                addICCCallEdge(replyTargetComponent, sourceICCInfo, targetICCInfo.info().getVar().getMethod());
                            }
                        })
                );
            }
        });
    }

    private void addICCCallEdge(JClass targetComponent, ICCInfo sourceICCInfo, JMethod callee) {
        CSObj recvObj = csManager.getCSObj(emptyContext, handlerContext.androidObjManager().getComponentObj(targetComponent));
        Context calleeCtx = selector.selectContext(sourceICCInfo.iccCSCallSite(), recvObj, callee);
        CSMethod csCallee = csManager.getCSMethod(calleeCtx, callee);
        addICCCallEdge(sourceICCInfo, csCallee, recvObj);
    }

    private void addICCCallEdge(ICCInfo sourceICCInfo, CSMethod callee, CSObj recvObj) {
        // build call edge
        solver.addCallEdge(new ICCCallEdge(sourceICCInfo, callee));
        // pass receiver object to *this* variable
        solver.addVarPointsTo(callee.getContext(), callee.getMethod().getIR().getThis(), recvObj);
    }

    private Set<JClass> transferVarToClass(CSVar component) {
        return solver.getPointsToSetOf(component)
                .objects()
                .filter(csObj -> csObj.getObject().getType() instanceof ClassType)
                .map(csObj -> csObj.getObject().getType())
                .map(type -> ((ClassType) type).getJClass())
                .collect(Collectors.toSet());
    }

    private void processHandlerSendMsgOrMessengerReplyMsg() {
        handlerContext.sendMessage().forEach((csObj, source) -> {
            if (!processedICCInfos.contains(source)) {
                handlerContext.handleMessage().get(csObj)
                        .stream()
                        .filter(target -> source.kind().equals(target.kind()))
                        .forEach(target -> sendMessage(source, target));
            }
        });
    }

    private void sendMessage(ICCInfo source, ICCInfo target) {
        CSObj recvObj = target.handlerObj();
        JMethod callee = target.info().getVar().getMethod();
        Context calleeCtx = selector.selectContext(source.iccCSCallSite(), recvObj, callee);
        CSMethod csCallee = csManager.getCSMethod(calleeCtx, callee);
        addICCCallEdge(source, csCallee, recvObj);
    }

    private Set<JClass> getTargetComponents(CSObj csObj, boolean isStartActivity) {
        return getMatchResult(transferIntentInfo(handlerContext.intent2IntentInfo().get(csObj), isStartActivity));
    }

    private void clearICCInfos() {
        Predicate<Map.Entry<CSVar, ICCInfo>> predicate = entry -> processedICCInfos.contains(entry.getValue());
        clearICCInfos(handlerContext.sourceComponent2ICCInfo(), predicate);
        clearICCInfos(handlerContext.targetComponent2ICCInfo(), predicate);
    }

    private void clearICCInfos(MultiMap<CSVar, ICCInfo> map, Predicate<Map.Entry<CSVar, ICCInfo>> predicate) {
        Set<Map.Entry<CSVar, ICCInfo>> set = map.entrySet()
                .stream()
                .filter(predicate)
                .collect(Collectors.toSet());
        set.forEach(entry -> map.remove(entry.getKey(), entry.getValue()));
    }

    private Set<JClass> getSourceComponentByTargetComponent(Set<JClass> targets) {
        return handlerContext.componentICCGraph().entrySet()
                .stream()
                .filter(entry -> targets.contains(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private TransferFilterInfo transferDynamicIntentFilterInfo(Set<IntentInfo> infos) {
        TransferFilterInfo dynamicFilterInfo = transferIntentInfo(infos, false);
        Set<String> schemes = Sets.newSet();
        Set<String> hosts = Sets.newSet();
        Set<String> ports = Sets.newSet();
        Set<String> paths = Sets.newSet();
        Set<String> mimeTypes = Sets.newSet();
        for (IntentInfo info : infos) {
            switch (info.kind()) {
                case DATA_SCHEME -> schemes.addAll(transferConstantObj(info.csVar().get(0)));
                case DATA_HOST -> hosts.addAll(transferConstantObj(info.csVar().get(0)));
                case DATA_PORT -> ports.addAll(transferConstantObj(info.csVar().get(0)));
                case DATA_PATH -> paths.addAll(transferConstantObj(info.csVar().get(0)));
                case MIME_TYPE -> mimeTypes.addAll(transferConstantObj(info.csVar().get(0)));
            }
        }
        return new TransferFilterInfo(
                dynamicFilterInfo.classNames(),
                dynamicFilterInfo.actions(),
                dynamicFilterInfo.categories(),
                new TransferDataInfo(
                        schemes,
                        hosts,
                        ports,
                        paths,
                        Sets.newSet(),
                        Sets.newSet(),
                        Sets.newSet(),
                        Sets.newSet(),
                        mimeTypes).convertToDataSet());
    }

    private TransferFilterInfo transferIntentInfo(Set<IntentInfo> infos, boolean isStartActivity) {
        Set<String> classNames = Sets.newSet();
        Set<String> actions = Sets.newSet();
        Set<String> categories = Sets.newSet();
        Set<UriData> data = Sets.newSet();
        for (IntentInfo info : infos) {
            switch (info.kind()) {
                case CLASS -> classNames.addAll(transferConstantObj(info.csVar().get(0)));
                case COMPONENT_NAME -> classNames.addAll(transferComponentName(info.csVar().get(0)));
                case ACTION -> actions.addAll(transferConstantObj(info.csVar().get(0)));
                case CATEGORY -> categories.addAll(transferConstantObj(info.csVar().get(0)));
                case DATA, NORMALIZE_DATA, MIME_TYPE, NORMALIZE_MIME_TYPE -> data.addAll(transferData(info.csVar().get(0), info.kind()));
                case DATA_AND_MIME_TYPE ->
                    // data and mimeType need to be satisfied at the same time, so needs to merge the information
                        data.addAll(handlerContext.intentInfoMatcher().mergeData(
                                transferData(info.csVar().get(0), IntentInfoKind.DATA),
                                transferData(info.csVar().get(1), IntentInfoKind.MIME_TYPE)));
                case NORMALIZE_DATA_AND_NORMALIZE_MIME_TYPE ->
                    // data and mimeType need to be satisfied at the same time, so needs to merge the information
                        data.addAll(handlerContext.intentInfoMatcher().mergeData(
                                transferData(info.csVar().get(0), IntentInfoKind.NORMALIZE_DATA),
                                transferData(info.csVar().get(1), IntentInfoKind.NORMALIZE_MIME_TYPE)));
            }
        }
        if (isStartActivity) {
            categories.add(DEFAULT_CATEGORY);
        }
        return new TransferFilterInfo(classNames, actions, categories, data);
    }

    private Set<JClass> getMatchResult(TransferFilterInfo userFilterInfo) {
        return handlerContext.intentInfoMatcher().getMatchResult(userFilterInfo,
                        getDynamicReceiverMatch(userFilterInfo))
                .stream()
                .map(hierarchy::getClass)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Set<String> getDynamicReceiverMatch(TransferFilterInfo userFilterInfo) {
        return handlerContext.dynamicReceiver2IntentFilter()
                .entrySet()
                .stream()
                .flatMap(entry -> solver.getPointsToSetOf(entry.getValue()).getObjects().stream()
                        .filter(csObj -> handlerContext.intentInfoMatcher().matchIntentFilter(
                                transferDynamicIntentFilterInfo(handlerContext.intentFilter2IntentInfo().get(csObj)),
                                userFilterInfo))
                        .map(csObj -> entry.getKey().getName()))
                .collect(Collectors.toSet());
    }

    private Set<String> transferComponentName(CSVar component) {
        return solver.getPointsToSetOf(component)
                .objects()
                .flatMap(csObj -> handlerContext.componentName2Info().get(csObj).stream())
                .flatMap(csVar -> transferConstantObj(csVar).stream())
                .collect(Collectors.toSet());
    }

    private Set<String> transferConstantObj(CSVar csVar) {
        return solver.getPointsToSetOf(csVar)
                .objects()
                .map(CSObj::getObject)
//                .filter(object -> object instanceof ConstantObj)
                .map(Obj::getAllocation)
                .map(allocation -> {
                    if (allocation instanceof StringLiteral stringLiteral) {
                        return stringLiteral.getString();
                    } else if (allocation instanceof ClassLiteral classLiteral) {
                        return classLiteral.getTypeValue().getName();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Set<UriData> transferData(CSVar csVar, IntentInfoKind kind) {
        Set<UriData> data = Sets.newSet();
        // <scheme>://<host>:<port>[<path>|<pathPrefix>|<pathPattern>|<pathAdvancedPattern>|<pathSuffix>]
        transferConstantObj(csVar).forEach(uriData -> {
            try {
                UriData d = null;
                if (kind == IntentInfoKind.DATA || kind == IntentInfoKind.NORMALIZE_DATA) {
                    URI uri = new URI(uriData);
                    String scheme = uri.getScheme();
                    if (scheme != null && kind == IntentInfoKind.NORMALIZE_DATA) {
                        scheme = normalizeScheme(scheme);
                    }
                    String host = uri.getHost().isEmpty() ? null : uri.getHost();
                    String port = uri.getPort() == -1 ? null : String.valueOf(uri.getPort());
                    String path = uri.getPath().isEmpty() ? null : uri.getPath();
                    d = UriData.builder()
                            .scheme(scheme)
                            .host(host)
                            .port(port)
                            .path(path)
                            .build();
                } else if (kind == IntentInfoKind.MIME_TYPE || kind == IntentInfoKind.NORMALIZE_MIME_TYPE) {
                    String mimeType = uriData;
                    if (kind == IntentInfoKind.NORMALIZE_MIME_TYPE) {
                        mimeType = normalizeMimeType(mimeType);
                    }
                    d = UriData.builder()
                            .mimeType(mimeType)
                            .build();
                }

                if (d != null) {
                    data.add(d);
                }
            } catch (Exception ignored) {
            }
        });
        return data;
    }

}
