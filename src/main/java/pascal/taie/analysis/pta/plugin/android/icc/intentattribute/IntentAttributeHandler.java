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

package pascal.taie.analysis.pta.plugin.android.icc.intentattribute;

import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.plugin.android.AndroidModelEdge;
import pascal.taie.analysis.pta.plugin.android.icc.ICCContext;
import pascal.taie.analysis.pta.plugin.android.icc.ICCHandler;
import pascal.taie.analysis.pta.plugin.android.icc.SendAndReplyICCHandler;
import pascal.taie.analysis.pta.plugin.util.InvokeHandler;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.TwoKeyMap;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static pascal.taie.analysis.pta.plugin.android.icc.intentattribute.IntentAttributeKind.ACTION;
import static pascal.taie.analysis.pta.plugin.android.icc.intentattribute.IntentAttributeKind.CATEGORY;
import static pascal.taie.analysis.pta.plugin.android.icc.intentattribute.IntentAttributeKind.CLASS;
import static pascal.taie.analysis.pta.plugin.android.icc.intentattribute.IntentAttributeKind.COMPONENT_NAME;
import static pascal.taie.analysis.pta.plugin.android.icc.intentattribute.IntentAttributeKind.DATA;
import static pascal.taie.analysis.pta.plugin.android.icc.intentattribute.IntentAttributeKind.DATA_AND_MIME_TYPE;
import static pascal.taie.analysis.pta.plugin.android.icc.intentattribute.IntentAttributeKind.MIME_TYPE;
import static pascal.taie.analysis.pta.plugin.android.icc.intentattribute.IntentAttributeKind.NORMALIZE_DATA;
import static pascal.taie.analysis.pta.plugin.android.icc.intentattribute.IntentAttributeKind.NORMALIZE_DATA_AND_NORMALIZE_MIME_TYPE;
import static pascal.taie.analysis.pta.plugin.android.icc.intentattribute.IntentAttributeKind.NORMALIZE_MIME_TYPE;
import static pascal.taie.analysis.pta.plugin.util.InvokeUtils.BASE;

/**
 * Tracks Intent attributes written through constructors/setters and propagates
 * them back through simple getters.
 *
 * <p>These facts are later normalized by {@link SendAndReplyICCHandler} when
 * resolving implicit ICC targets against manifest and dynamic intent filters.
 */
public class IntentAttributeHandler extends ICCHandler {

    private static final String INIT_WITH_ACTION_AND_DATA = "<android.content.Intent: void <init>(java.lang.String,android.net.Uri)>";

    private static final String INIT_WITH_ACTION_AND_DATA_AND_CLASS = "<android.content.Intent: void <init>(java.lang.String,android.net.Uri,android.content.Context,java.lang.Class)>";

    private static final String INIT_WITH_INTENT = "<android.content.Intent: void <init>(android.content.Intent)>";

    private static final String INIT_WITH_ACTION = "<android.content.Intent: void <init>(java.lang.String)>";

    private static final String INIT_WITH_TARGET_COMPONENT = "<android.content.Intent: void <init>(android.content.Context,java.lang.Class)>";

    private static final String SET_CLASS = "<android.content.Intent: android.content.Intent setClass(android.content.Context,java.lang.Class)>";

    private static final String SET_CLASS_NAME_WITH_CONTEXT = "<android.content.Intent: android.content.Intent setClassName(android.content.Context,java.lang.String)>";

    private static final String SET_CLASS_NAME_WITH_PACKAGE = "<android.content.Intent: android.content.Intent setClassName(java.lang.String,java.lang.String)>";

    private static final String SET_ACTION = "<android.content.Intent: android.content.Intent setAction(java.lang.String)>";

    private static final String SET_COMPONENT = "<android.content.Intent: android.content.Intent setComponent(android.content.ComponentName)>";

    private static final String ADD_CATEGORY = "<android.content.Intent: android.content.Intent addCategory(java.lang.String)>";

    private static final String SET_DATA = "<android.content.Intent: android.content.Intent setData(android.net.Uri)>";

    private static final String SET_DATA_AND_NORMALIZE = "<android.content.Intent: android.content.Intent setDataAndNormalize(android.net.Uri)>";

    private static final String SET_DATA_AND_TYPE = "<android.content.Intent: android.content.Intent setDataAndType(android.net.Uri,java.lang.String)>";

    private static final String SET_DATA_AND_TYPE_AND_NORMALIZE = "<android.content.Intent: android.content.Intent setDataAndTypeAndNormalize(android.net.Uri,java.lang.String)>";

    private static final String SET_TYPE = "<android.content.Intent: android.content.Intent setType(java.lang.String)>";

    private static final String SET_TYPE_AND_NORMALIZE = "<android.content.Intent: android.content.Intent setTypeAndNormalize(java.lang.String)>";

    private static final String URI_PARSE = "<android.net.Uri: android.net.Uri parse(java.lang.String)>";

    private static final String GET_ACTION = "<android.content.Intent: java.lang.String getAction()>";

    private static final String GET_CATEGORIES = "<android.content.Intent: java.util.Set getCategories()>";

    private static final String GET_DATA = "<android.content.Intent: android.net.Uri getData()>";

    private static final String GET_TYPE = "<android.content.Intent: java.lang.String getType()>";

    /**
     * Pending Intent attribute getter results grouped by receiver object and result
     * variable. Getter results are resolved at phase finish so they can observe
     * attributes written before or after the getter call.
     */
    private final TwoKeyMap<CSObj, CSVar, List<IntentAttributeKind>> pendingIntentAttributeGets =
            Maps.newTwoKeyMap();

    public IntentAttributeHandler(ICCContext context) {
        super(context);
    }

    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        CSCallSite csCallSite = edge.getCallSite();
        Context callerCtx = csCallSite.getContext();
        Invoke callSite = csCallSite.getCallSite();
        JMethod callee = edge.getCallee().getMethod();
        Var result = callSite.getResult();
        if (result != null && callee.getSignature().equals(URI_PARSE)) {
            // Uri.parse(String) preserves the string value for later Intent data matching.
            CSVar source = csManager.getCSVar(callerCtx, callSite.getInvokeExp().getArg(0));
            CSVar target = csManager.getCSVar(callerCtx, result);
            solver.addPFGEdge(new AndroidModelEdge(source, target));
        }
    }

    @InvokeHandler(signature = {
            INIT_WITH_INTENT,
            INIT_WITH_ACTION,
            INIT_WITH_TARGET_COMPONENT,
            INIT_WITH_ACTION_AND_DATA,
            INIT_WITH_ACTION_AND_DATA_AND_CLASS,
            SET_CLASS,
            SET_CLASS_NAME_WITH_CONTEXT,
            SET_CLASS_NAME_WITH_PACKAGE,
            SET_ACTION,
            SET_COMPONENT,
            ADD_CATEGORY,
            SET_DATA,
            SET_DATA_AND_NORMALIZE,
            SET_TYPE,
            SET_TYPE_AND_NORMALIZE,
            SET_DATA_AND_TYPE,
            SET_DATA_AND_TYPE_AND_NORMALIZE},
            argIndexes = {BASE})
    public void intentCommonInvoke(Context context, Invoke invoke, PointsToSet intentObjs) {
        JMethod method = invoke.getMethodRef().resolve();
        InvokeExp invokeExp = invoke.getInvokeExp();
        CSVar base = csManager.getCSVar(context, InvokeUtils.getVar(invoke, BASE));
        IntentAttributeCollector collector =
                new IntentAttributeCollector(context, csManager, invokeExp);
        if (INIT_WITH_INTENT.equals(method.getSignature())) {
            solver.addPFGEdge(new AndroidModelEdge(collector.arg(0), base), base.getType());
        }
        List<IntentAttribute> intentAttributes =
                collectWrittenIntentAttributes(method.getSignature(), collector);
        if (!intentAttributes.isEmpty()) {
            intentObjs.forEach(csObj -> handlerContext.intent2IntentAttribute().putAll(csObj, intentAttributes));
        }

        Var result = invoke.getResult();
        if (result != null) {
            solver.addPFGEdge(new AndroidModelEdge(base, csManager.getCSVar(context, result)));
        }
    }

    @InvokeHandler(signature = {
            GET_ACTION,
            GET_CATEGORIES,
            GET_DATA,
            GET_TYPE},
            argIndexes = {BASE})
    public void intentGetIntentAttribute(Context context, Invoke invoke, PointsToSet intentObjs) {
        JMethod method = invoke.getMethodRef().resolve();
        Var result = invoke.getResult();
        if (result == null) {
            return;
        }

        CSVar target = csManager.getCSVar(context, result);
        List<IntentAttributeKind> kinds = getGetterKinds(method.getSignature());
        if (!kinds.isEmpty()) {
            intentObjs.forEach(intentObj ->
                    pendingIntentAttributeGets.put(intentObj, target, kinds)
            );
        }
    }

    @Override
    public void onPhaseFinish() {
        pendingIntentAttributeGets.entrySet().forEach(entry -> {
            CSObj intentObj = entry.key1();
            CSVar target = entry.key2();
            List<IntentAttributeKind> kinds = entry.value();
            connectGetIntentAttribute(
                    processGetIntentAttribute(intentObj, info -> kinds.contains(info.kind())),
                    target);
        });
    }

    private Set<CSVar> processGetIntentAttribute(CSObj csObj, Predicate<IntentAttribute> predicate) {
        return handlerContext.intent2IntentAttribute().get(csObj)
                .stream()
                .filter(predicate)
                .flatMap(info -> info.csVar().stream())
                .collect(Collectors.toSet());
    }

    private void connectGetIntentAttribute(Set<CSVar> sources, CSVar result) {
        sources.forEach(source ->
                solver.addPFGEdge(new AndroidModelEdge(source, result), result.getType()));
    }

    private void copyIntentArgToBaseIfNeeded(String signature,
                                             IntentAttributeCollector collector,
                                             CSVar base) {
        if (INIT_WITH_INTENT.equals(signature)) {
            solver.addPFGEdge(new AndroidModelEdge(collector.arg(0), base), base.getType());
        }
    }

    private List<IntentAttribute> collectWrittenIntentAttributes(String signature,
                                                                 IntentAttributeCollector collector) {
        switch (signature) {
            case INIT_WITH_ACTION, SET_ACTION -> collector.addSingle(0, ACTION);
            case INIT_WITH_ACTION_AND_DATA -> {
                collector.addSingle(0, ACTION);
                collector.addSingle(1, DATA);
            }
            case INIT_WITH_ACTION_AND_DATA_AND_CLASS -> {
                collector.addSingle(0, ACTION);
                collector.addSingle(1, DATA);
                collector.addSingle(3, CLASS);
            }
            case INIT_WITH_TARGET_COMPONENT,
                    SET_CLASS,
                    SET_CLASS_NAME_WITH_CONTEXT,
                    SET_CLASS_NAME_WITH_PACKAGE -> collector.addSingle(1, CLASS);
            case SET_COMPONENT -> collector.addSingle(0, COMPONENT_NAME);
            case ADD_CATEGORY -> collector.addSingle(0, CATEGORY);
            case SET_DATA -> collector.addSingle(0, DATA);
            case SET_DATA_AND_NORMALIZE -> collector.addSingle(0, NORMALIZE_DATA);
            case SET_TYPE -> collector.addSingle(0, MIME_TYPE);
            case SET_TYPE_AND_NORMALIZE -> collector.addSingle(0, NORMALIZE_MIME_TYPE);
            case SET_DATA_AND_TYPE -> collector.addPair(0, 1, DATA_AND_MIME_TYPE);
            case SET_DATA_AND_TYPE_AND_NORMALIZE ->
                    collector.addPair(0, 1, NORMALIZE_DATA_AND_NORMALIZE_MIME_TYPE);
        }
        return collector.attributes();
    }

    private static List<IntentAttributeKind> getGetterKinds(String signature) {
        return switch (signature) {
            case GET_ACTION -> List.of(ACTION);
            case GET_CATEGORIES -> List.of(CATEGORY);
            case GET_DATA -> List.of(
                    DATA,
                    NORMALIZE_DATA,
                    DATA_AND_MIME_TYPE,
                    NORMALIZE_DATA_AND_NORMALIZE_MIME_TYPE);
            case GET_TYPE -> List.of(
                    MIME_TYPE,
                    NORMALIZE_MIME_TYPE,
                    DATA_AND_MIME_TYPE,
                    NORMALIZE_DATA_AND_NORMALIZE_MIME_TYPE);
            default -> List.of();
        };
    }
}
