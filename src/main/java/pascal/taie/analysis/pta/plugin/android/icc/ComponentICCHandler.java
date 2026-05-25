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
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Subsignature;
import pascal.taie.language.type.ClassType;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import java.util.Map;

import static pascal.taie.analysis.pta.plugin.util.InvokeUtils.BASE;
import static pascal.taie.android.AndroidClassNames.ACTIVITY;
import static pascal.taie.android.AndroidClassNames.BROADCAST_RECEIVER;
import static pascal.taie.android.AndroidClassNames.SERVICE;

/**
 * Records incoming component lifecycle entry points and service-binding facts.
 *
 * <p>Source-side ICC calls are recorded by {@link StartICCModel}. This handler
 * records target-side lifecycle parameters such as Activity.getIntent(),
 * Service.onBind(...), Service.onStartCommand(...), and
 * BroadcastReceiver.onReceive(...). {@link SendAndReplyICCHandler} later joins
 * the two sides.
 */
public class ComponentICCHandler extends ICCHandler {

    private static final Subsignature ON_ACTIVITY_RESULT_SUB_SIG = Subsignature.get("void onActivityResult(int,int,android.content.Intent)");

    private static final Subsignature ON_BIND_SUB_SIG = Subsignature.get("android.os.IBinder onBind(android.content.Intent)");

    private static final Subsignature ON_START_COMMAND = Subsignature.get("int onStartCommand(android.content.Intent,int,int)");

    private static final Subsignature ON_HANDLE_INTENT = Subsignature.get("void onHandleIntent(android.content.Intent)");

    private static final Subsignature ON_RECEIVE = Subsignature.get("void onReceive(android.content.Context,android.content.Intent)");

    private static final Subsignature ON_SERVICE_CONNECTED_SUB_SIG = Subsignature.get("void onServiceConnected(android.content.ComponentName,android.os.IBinder)");

    private static final String GET_BINDER = "<android.os.Messenger: android.os.IBinder getBinder()>";

    private static final String GET_INTENT = "<android.app.Activity: android.content.Intent getIntent()>";

    /**
     * Maps IBinder objects returned by Service.onBind(...) back to the Messenger
     * object whose getBinder() result produced that binder.
     */
    private final MultiMap<CSObj, CSVar> onBindResult2Messenger = Maps.newMultiMap();

    public ComponentICCHandler(ICCContext context) {
        super(context);
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        // A bindService(...) ServiceConnection object may become available after the call is seen.
        handlerContext.intents2ServiceConnection().forEach((intent, serviceConnection) -> {
            if (serviceConnection.equals(csVar)) {
                pts.forEach(thisObj -> processServiceConnectionVar(intent, thisObj.getObject()));
            }
        });

        // onBind(...) return objects connect the service component to IBinder/Messenger facts.
        processOnBindMethodReturnVar(csVar, pts);
    }

    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        CSMethod csCallee = edge.getCallee();
        Context callerCtx = edge.getCallSite().getContext();
        Invoke callSite = edge.getCallSite().getCallSite();
        JMethod callee = csCallee.getMethod();
        JMethod caller = callSite.getContainer();
        if (caller.isStatic()) {
            return;
        }

        CSVar callerThisCSVar = csManager.getCSVar(callerCtx, caller.getIR().getThis());
        // Activity.getIntent() is the target-side Intent receiver for activity ICC.
        if (callee.getSignature().equals(GET_INTENT) && callSite.getResult() != null) {
            CSVar intent = csManager.getCSVar(callerCtx, callSite.getResult());
            // The same getIntent() result may receive either startActivity or startActivityForResult intents.
            handlerContext.targetComponent2ICCInfo().put(callerThisCSVar, new ICCInfo(intent, ICCInfoKind.START_ACTIVITY_FOR_RESULT, null, null));
            handlerContext.targetComponent2ICCInfo().put(callerThisCSVar, new ICCInfo(intent, ICCInfoKind.START_ACTIVITY, null, null));
        }
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        Context context = csMethod.getContext();
        JMethod method = csMethod.getMethod();
        if (method.isStatic()) {
            return;
        }

        CSVar component = csManager.getCSVar(context, method.getIR().getThis());
        CSVar intent = null;
        ICCInfoKind kind = ICCInfoKind.OTHER;
        if (handlerContext.lifecycleHelper().isLifeCycleMethod(method, ACTIVITY, ON_ACTIVITY_RESULT_SUB_SIG)) {
            // Activity.onActivityResult receives the reply Intent produced by setResult(...).
            intent = csManager.getCSVar(context, method.getIR().getParam(2));
            kind = ICCInfoKind.START_ACTIVITY_FOR_RESULT_REPLY;
        } else if (handlerContext.lifecycleHelper().isLifeCycleMethod(method, SERVICE, ON_BIND_SUB_SIG)) {
            // Service.onBind receives the binding Intent and may return an IBinder.
            // process onBind CSMethod in service component
            processGetBinder(context, method);
            intent = csManager.getCSVar(context, method.getIR().getParam(0));
            kind = ICCInfoKind.BIND_SERVICE;
        } else if (handlerContext.lifecycleHelper().isLifeCycleMethod(method, SERVICE, ON_START_COMMAND)) {
            // Service starts receive their Intent through onStartCommand/onHandleIntent.
            intent = csManager.getCSVar(context, method.getIR().getParam(0));
            kind = ICCInfoKind.START_SERVICE;
        } else if (handlerContext.lifecycleHelper().isLifeCycleMethod(method, SERVICE, ON_HANDLE_INTENT)) {
            intent = csManager.getCSVar(context, method.getIR().getParam(0));
            kind = ICCInfoKind.START_SERVICE;
        } else if (handlerContext.lifecycleHelper().isLifeCycleMethod(method, BROADCAST_RECEIVER, ON_RECEIVE)) {
            // Broadcast receivers receive broadcast Intents through onReceive(...).
            intent = csManager.getCSVar(context, method.getIR().getParam(1));
            kind = ICCInfoKind.SEND_BROADCAST;
        }
        if (intent != null) {
            recordTargetSideICC(component, new ICCInfo(intent, kind, null, null));
        }
    }

    private void recordTargetSideICC(CSVar component, ICCInfo iccInfo) {
        // For replay intents, the target component corresponds to the original sender (source component).
        if (iccInfo.kind().equals(ICCInfoKind.START_ACTIVITY_FOR_RESULT_REPLY)) {
            handlerContext.sourceComponent2ICCInfo().put(component, iccInfo);
        } else {
            handlerContext.targetComponent2ICCInfo().put(component, iccInfo);
        }
    }

    /**
     * Finds Messenger.getBinder() results inside Service.onBind(...). When the
     * returned IBinder object later appears in the return points-to set, we can
     * recover the Messenger associated with the service component.
     */
    private void processGetBinder(Context context, JMethod onBind) {
        onBind.getIR().getStmts()
                .stream()
                .filter(stmt -> stmt instanceof Invoke invoke && !invoke.isDynamic() && !invoke.isStatic())
                .map(stmt -> (Invoke) stmt)
                .forEach(invoke -> {
                    JMethod resolve = invoke.getMethodRef().resolveNullable();
                    Var base = InvokeUtils.getVar(invoke, BASE);
                    if (resolve != null && resolve.getSignature().equals(GET_BINDER)) {
                        CSVar messengerCSVar = csManager.getCSVar(context, base);
                        CSObj result = addResultObjectForInvoke(context, invoke);
                        if (result != null) {
                            onBindResult2Messenger.put(result, messengerCSVar);
                        }
                    }
                });
    }

    /**
     * Adds ServiceConnection lifecycle entry points and records the IBinder
     * parameter of onServiceConnected(...) as the binding result for the Intent.
     */
    private void processServiceConnectionVar(CSVar intent, Obj thisObj) {
        if (thisObj.getType() instanceof ClassType classType) {
            handlerContext.lifecycleHelper()
                    .getLifeCycleMethods(classType.getJClass())
                    .forEach(serviceConnectionMethod -> {
                        Map<Integer, Obj> paramIndex = Maps.newMap();
                        if (serviceConnectionMethod.getSubsignature().equals(ON_SERVICE_CONNECTED_SUB_SIG)) {
                            Obj param = handlerContext.androidObjManager().mockLifecycleMethodParamObj(serviceConnectionMethod, serviceConnectionMethod.getIR().getParam(1));
                            paramIndex.put(1, param);
                            handlerContext.intent2IBinder().put(intent, csManager.getCSVar(emptyContext, serviceConnectionMethod.getIR().getParam(1)));
                        }
                        addEntryPoint(serviceConnectionMethod, thisObj, paramIndex);
                    });
        }
    }

    /**
     * Connects Service.onBind(...) return objects to the service component. The
     * result may be consumed directly as an IBinder or indirectly through
     * Messenger.getBinder().
     */
    private void processOnBindMethodReturnVar(CSVar csVar, PointsToSet pts) {
        Context context = csVar.getContext();
        Var var = csVar.getVar();
        JMethod container = var.getMethod();
        if (handlerContext.lifecycleHelper().isLifeCycleMethod(container, SERVICE, ON_BIND_SUB_SIG) && container.getIR().getReturnVars().contains(var)) {
            CSVar thisCSVar = csManager.getCSVar(context, container.getIR().getThis());
            pts.forEach(csObj -> {
                if (onBindResult2Messenger.containsKey(csObj)) {
                    handlerContext.serviceComponent2Messenger().putAll(thisCSVar, onBindResult2Messenger.get(csObj));
                }
                handlerContext.serviceComponent2IBinder().put(thisCSVar, csObj);
            });
        }
    }

}
