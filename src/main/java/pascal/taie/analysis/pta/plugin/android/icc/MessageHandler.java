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
import pascal.taie.analysis.pta.core.cs.element.InstanceField;
import pascal.taie.analysis.pta.plugin.android.AndroidModelEdge;
import pascal.taie.analysis.pta.plugin.util.InvokeHandler;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Subsignature;
import pascal.taie.language.type.ClassType;

import java.util.Set;

import static pascal.taie.analysis.pta.plugin.util.InvokeUtils.BASE;

/**
 * Models Handler/Messenger based ICC.
 *
 * <p>Handler.sendMessage(...) and Messenger.send(...) are treated as ICC-like
 * dispatches. This handler records message sources and handler targets;
 * {@link SendAndReplyICCHandler} later joins matching send/handle facts.
 */
public class MessageHandler extends ICCHandler {

    private static final Subsignature HANDLE_MESSAGE_SUB_SIG = Subsignature.get("void handleMessage(android.os.Message)");

    private static final String INIT_WITH_IBINDER = "<android.os.Messenger: void <init>(android.os.IBinder)>";

    private static final String SEND_MESSAGE = "<android.os.Messenger: void send(android.os.Message)>";

    private static final String MESSAGE_OBTAIN = "<android.os.Message: android.os.Message obtain(android.os.Handler,int,int,int)>";

    private static final String MESSAGE_OBTAIN2 = "<android.os.Message: android.os.Message obtain(android.os.Handler,int,java.lang.Object)>";

    public MessageHandler(ICCContext context) {
        super(context);
    }

    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        JMethod callee = edge.getCallee().getMethod();
        CSCallSite csCallSite = edge.getCallSite();
        Context context = csCallSite.getContext();
        Invoke callSite = csCallSite.getCallSite();
        switch (callee.getSignature()) {
            case MESSAGE_OBTAIN -> {
                CSObj messageObj = addResultObjectForInvoke(context, callSite);
                JField field = hierarchy.getField("<android.os.Message: int arg1>");
                if (field != null && messageObj != null) {
                    InstanceField instField = csManager.getInstanceField(messageObj, field);
                    CSVar arg = csManager.getCSVar(context, callSite.getInvokeExp().getArg(2));
                    solver.addPFGEdge(new AndroidModelEdge(arg, instField));
                }
            }
            case MESSAGE_OBTAIN2 -> {
                CSObj messageObj = addResultObjectForInvoke(context, callSite);
                JField field = hierarchy.getField("<android.os.Message: java.lang.Object obj>");
                if (field != null && messageObj != null) {
                    InstanceField instField = csManager.getInstanceField(messageObj, field);
                    CSVar arg = csManager.getCSVar(context, callSite.getInvokeExp().getArg(2));
                    solver.addPFGEdge(new AndroidModelEdge(arg, instField));
                }
            }
        }
    }

    @InvokeHandler(signature = {
            "<android.os.Handler: boolean sendMessage(android.os.Message)>",
            "<android.os.Handler: void dispatchMessage(android.os.Message)>"},
            argIndexes = {BASE})
    public void handlerSendMessage(Context context, Invoke invoke, PointsToSet pts) {
        Var arg = invoke.getInvokeExp().getArg(0);
        pts.forEach(csObj -> {
            if (csObj.getObject().getType() instanceof ClassType classType) {
                processSendMessage(context, csObj, arg, invoke);
                processHandleMessage(classType.getJClass(), Set.of(csObj), csObj);
            }
        });
    }

    @InvokeHandler(signature = {
            INIT_WITH_IBINDER,
            SEND_MESSAGE},
            argIndexes = {BASE})
    public void messengerCommonInvoke(Context context, Invoke invoke, PointsToSet pts) {
        JMethod method = invoke.getMethodRef().resolve();
        Var arg = invoke.getInvokeExp().getArg(0);
        switch (method.getSignature()) {
            case INIT_WITH_IBINDER -> processInitMessengerWithIBinder(context, pts, arg);
            case SEND_MESSAGE -> processSendMessage(context, pts, arg, invoke);
        }
    }

    @InvokeHandler(signature = "<android.os.Messenger: void <init>(android.os.Handler)>", argIndexes = {BASE, 0})
    public void messengerInitWithHandler(Context context, Invoke invoke, PointsToSet messengerObjs, PointsToSet handlerObjs) {
        Var handler = invoke.getInvokeExp().getArg(0);
        processInitMessengerWithHandler(context, messengerObjs, handler);
        handlerObjs.forEach(thisObj -> {
            if (thisObj.getObject().getType() instanceof ClassType classType) {
                processHandleMessage(classType.getJClass(), messengerObjs.getObjects(), thisObj);
            }
        });
     }

    private void processInitMessengerWithHandler(Context context, PointsToSet pts, Var arg) {
        pts.forEach(csObj -> handlerContext.messenger2Handler().put(csObj, csManager.getCSVar(context, arg)));
    }

    private void processInitMessengerWithIBinder(Context context, PointsToSet pts, Var arg) {
        pts.forEach(csObj -> handlerContext.messenger2IBinder().put(csObj, csManager.getCSVar(context, arg)));
    }

    private void processSendMessage(Context context, PointsToSet pts, Var arg, Invoke invoke) {
        pts.forEach(csObj -> processSendMessage(context, csObj, arg, invoke));
    }

    private void processSendMessage(Context context, CSObj csObj, Var arg, Invoke invoke) {
        handlerContext.sendMessage().put(csObj, new ICCInfo(csManager.getCSVar(context, arg), ICCInfoKind.MESSENGER, null, csManager.getCSCallSite(context, invoke)));
    }

    private void processHandleMessage(JClass handler, Set<CSObj> csObjs, CSObj thisObj) {
        handlerContext.lifecycleHelper()
                .getLifeCycleMethods(handler)
                .forEach(handlerMsgMethod -> {
                    if (handlerMsgMethod.getSubsignature().equals(HANDLE_MESSAGE_SUB_SIG)) {
                        Var param = handlerMsgMethod.getIR().getParam(0);
                        for (CSObj csObj : csObjs) {
                            handlerContext.handleMessage().put(csObj, new ICCInfo(csManager.getCSVar(emptyContext, param), ICCInfoKind.MESSENGER, thisObj, null));
                        }
                    }
                    addEntryPoint(handlerMsgMethod, thisObj.getObject());
                });
    }

}
