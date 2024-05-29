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

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.plugin.util.InvokeHandler;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;

import static pascal.taie.analysis.pta.plugin.util.InvokeUtils.BASE;

/**
 * Models start ICC related methods.
 */
public class StartICCModel extends ICCHandler {

    private static final String START_ACTIVITY_SUB_SIG = "void startActivity(android.content.Intent)";

    private static final String START_ACTIVITY_FOR_RESULT_SUB_SIG = "void startActivityForResult(android.content.Intent,int)";

    private static final String SEND_BROADCAST_SUB_SIG = "void sendBroadcast(android.content.Intent)";

    private static final String BIND_SERVICE_SUB_SIG = "boolean bindService(android.content.Intent,android.content.ServiceConnection,int)";

    private static final String START_SERVICE_SUB_SIG = "android.content.ComponentName startService(android.content.Intent)";

    private static final String SET_RESULT_SUB_SIG = "void setResult(int,android.content.Intent)";

    private static final String ON_ACTIVITY_RESULT_IN_ACTIVITY = "<android.app.Activity: void onActivityResult(int,int,android.content.Intent)>";

    public StartICCModel(ICCContext context) {
        super(context);
    }

    @InvokeHandler(signature = {
            "<android.app.Activity: void startActivity(android.content.Intent)>",
            "<android.content.Context: void startActivity(android.content.Intent)>",
            "<android.content.ContextWrapper: void startActivity(android.content.Intent)>",
            "<android.app.Activity: void startActivityForResult(android.content.Intent,int)>",
            "<android.content.Context: void startActivityForResult(android.content.Intent,int)>",
            "<android.content.ContextWrapper: void startActivityForResult(android.content.Intent,int)>",
            "<android.content.Context: void sendBroadcast(android.content.Intent)>",
            "<android.content.ContextWrapper: void sendBroadcast(android.content.Intent)>",
            "<android.content.Context: boolean bindService(android.content.Intent,android.content.ServiceConnection,int)>",
            "<android.content.ContextWrapper: boolean bindService(android.content.Intent,android.content.ServiceConnection,int)>",
            "<android.content.Context: android.content.ComponentName startService(android.content.Intent)>",
            "<android.content.ContextWrapper: android.content.ComponentName startService(android.content.Intent)>",
            "<android.app.Activity: void setResult(int,android.content.Intent)>"},
            argIndexes = {BASE})
    public void iccInvoke(Context context, Invoke invoke, PointsToSet pts) {
        CSVar csVar = csManager.getCSVar(context, InvokeUtils.getVar(invoke, BASE));
        CSCallSite csCallSite = csManager.getCSCallSite(context, invoke);
        CSVar intent = csManager.getCSVar(context, invoke.getInvokeExp().getArg(0));
        String methodSubSig = invoke.getMethodRef().resolve().getSubsignature().toString();
        pts.forEach(senderClassObj -> {
            if (senderClassObj.getObject().getType() instanceof ClassType classType) {
                ICCInfo iccInfo = null;
                switch (methodSubSig) {
                    case START_ACTIVITY_SUB_SIG -> iccInfo = new ICCInfo(intent, ICCInfoKind.START_ACTIVITY, null, csCallSite);
                    case START_ACTIVITY_FOR_RESULT_SUB_SIG -> {
                        iccInfo = new ICCInfo(intent, ICCInfoKind.START_ACTIVITY_FOR_RESULT, null, csCallSite);
                        processOnActivityResult(classType.getJClass());
                    }
                    case SEND_BROADCAST_SUB_SIG -> iccInfo = new ICCInfo(intent, ICCInfoKind.SEND_BROADCAST, null, csCallSite);
                    case BIND_SERVICE_SUB_SIG -> {
                        iccInfo = new ICCInfo(intent, ICCInfoKind.BIND_SERVICE, null, csCallSite);
                        Var serviceConnectionArg= invoke.getInvokeExp().getArg(1);
                        handlerContext.intents2ServiceConnection().put(intent, csManager.getCSVar(context, serviceConnectionArg));
                    }
                    case START_SERVICE_SUB_SIG -> iccInfo = new ICCInfo(intent, ICCInfoKind.START_SERVICE, null, csCallSite);
                    case SET_RESULT_SUB_SIG -> {
                        CSVar replyIntent = csManager.getCSVar(context, invoke.getInvokeExp().getArg(1));
                        iccInfo = new ICCInfo(replyIntent, ICCInfoKind.START_ACTIVITY_FOR_RESULT_REPLY, null, csCallSite);
                    }
                }
                if (iccInfo != null) {
                    addComponentIntent(csVar, iccInfo);
                }
            }
        });
    }

    private void addComponentIntent(CSVar csVar, ICCInfo iccInfo) {
        (iccInfo.kind().equals(ICCInfoKind.START_ACTIVITY_FOR_RESULT_REPLY) ?
                handlerContext.targetComponent2ICCInfo() : handlerContext.sourceComponent2ICCInfo()).put(csVar, iccInfo);
    }

    private void processOnActivityResult(JClass decl) {
        JMethod onActivityResult = hierarchy.getMethod(ON_ACTIVITY_RESULT_IN_ACTIVITY);
        if (onActivityResult != null) {
            JMethod dispatch = hierarchy.dispatch(decl, onActivityResult.getRef());
            if (dispatch != null) {
                addEntryPoint(dispatch, handlerContext.androidObjManager().getComponentObj(decl));
            }
        }
    }

}
