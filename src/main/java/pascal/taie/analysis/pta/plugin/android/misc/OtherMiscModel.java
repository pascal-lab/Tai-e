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

package pascal.taie.analysis.pta.plugin.android.misc;

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.MockObj;
import pascal.taie.analysis.pta.core.heap.NewObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.EmptyParamProvider;
import pascal.taie.analysis.pta.core.solver.EntryPoint;
import pascal.taie.analysis.pta.plugin.android.AndroidTransferEdge;
import pascal.taie.analysis.pta.plugin.util.CSObjs;
import pascal.taie.analysis.pta.plugin.util.InvokeHandler;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.android.AndroidClassNames;
import pascal.taie.ir.exp.CastExp;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.StringLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import java.util.List;

import static pascal.taie.analysis.pta.plugin.util.InvokeUtils.BASE;

public class OtherMiscModel extends AndroidMiscHandler {

    private final static List<String> CAST_TYPE = List.of(
            AndroidClassNames.VIEW,
            AndroidClassNames.TEXT_VIEW,
            AndroidClassNames.URL_CONNECTION
    );

    private final JMethod RUNNABLE_RUN = hierarchy.getJREMethod("<java.lang.Runnable: void run()>");

    private final String GET_ACCOUNTS = "<android.accounts.AccountManager: android.accounts.Account[] getAccounts()>";

    private final MultiMap<CSObj, CSVar> singleMap = Maps.newMultiMap();

    private final MultiMap<CSObj, CSVar> getSingleMap = Maps.newMultiMap();

    private final MultiMap<CSVar, CSVar> castMap = Maps.newMultiMap();

    public OtherMiscModel(AndroidMiscContext specificContext) {
        super(specificContext);
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        Context context = csMethod.getContext();
        // propagate cast in some android system class type
        csMethod.getMethod().getIR().getStmts().forEach(stmt -> {
            if (stmt instanceof Cast c) {
                CastExp cast = c.getRValue();
                String valueType = cast.getValue().getType().getName();
                if (CAST_TYPE.contains(valueType)) {
                    CSVar from = csManager.getCSVar(context, cast.getValue());
                    CSVar to = csManager.getCSVar(context, c.getLValue());
                    castMap.put(from, to);
                }
            }
            if (stmt instanceof LoadField lf && lf.getFieldAccess() instanceof InstanceFieldAccess access) {
                JField jField = lf.getFieldRef().resolveNullable();
                if (jField != null && jField.getDeclaringClass().getName().equals(AndroidClassNames.ACCOUNT)) {
                    Var result = lf.getLValue();
                    solver.addPFGEdge(new AndroidTransferEdge(
                            csManager.getCSVar(context, access.getBase()),
                            csManager.getCSVar(context, result))
                    );
                }
            }
            if (stmt instanceof Invoke i && !i.isDynamic()) {
                JMethod jMethod = i.getMethodRef().resolveNullable();
                if (jMethod != null && jMethod.getSignature().equals(GET_ACCOUNTS)) {
                    generateInvokeResultObj(context, i);
                }
            }
        });

        // transfer account field
        csMethod.getMethod().getIR().getStmts().stream().filter(stmt -> stmt instanceof LoadField)
                .map(stmt -> (LoadField) stmt).forEach(stmt -> {
                    if (stmt.getFieldAccess() instanceof InstanceFieldAccess access) {
                        JField jField = stmt.getFieldRef().resolveNullable();
                        if (jField != null && jField.getDeclaringClass().getName().equals(AndroidClassNames.ACCOUNT)) {
                            Var result = stmt.getLValue();
                            solver.addPFGEdge(new AndroidTransferEdge(
                                    csManager.getCSVar(context, access.getBase()),
                                    csManager.getCSVar(context, result))
                            );
                        }
                    }
                });
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pointsToSet) {
        super.onNewPointsToSet(csVar, pointsToSet);
        castMap.get(csVar).forEach(to -> solver.addPointsTo(to, pointsToSet));
    }

    @Override
    public void onPhaseFinish() {
        processGetSingleMap();
    }

    @InvokeHandler(signature = "<java.lang.Class: java.lang.String getName()>", argIndexes = {BASE})
    public void classGetName(Context context, Invoke invoke, PointsToSet classes) {
        Var result = invoke.getResult();
        if (result != null) {
            classes.forEach(csObj -> solver.addVarPointsTo(context, result, csObj));
        }
    }

    @InvokeHandler(signature = "<java.lang.reflect.Method: java.lang.String getName()>", argIndexes = {BASE})
    public void methodGetName(Context context, Invoke invoke, PointsToSet classes) {
        Var result = invoke.getResult();
        if (result != null) {
            classes.forEach(csObj -> {
                JMethod method = CSObjs.toMethod(csObj);
                if (method != null) {
                    Obj methodName = heapModel.getConstantObj(StringLiteral.get(method.getName()));
                    solver.addVarPointsTo(context, result, methodName);
                }
            });
        }
    }

    @InvokeHandler(signature = "<java.lang.String: void <init>(java.lang.String)>", argIndexes = {0})
    public void initStringWithString(Context context, Invoke invoke, PointsToSet stringObjs) {
        CSVar csVar = csManager.getCSVar(context, InvokeUtils.getVar(invoke, BASE));
        stringObjs.forEach(csObj -> solver.addPointsTo(csVar, csObj));
    }

    @InvokeHandler(signature = {
            "<android.webkit.WebView: void addJavascriptInterface(java.lang.Object,java.lang.String)>"
    }, argIndexes = {0})
    public void addJavascriptInterface(Context context, Invoke invoke, PointsToSet argObjs) {
        CSVar csArg = csManager.getCSVar(context, InvokeUtils.getVar(invoke, 0));
        argObjs.forEach(argObj -> {
            if (argObj.getObject() instanceof NewObj newObj && newObj.getType() instanceof ClassType classType) {
                JClass jClass = classType.getJClass();
                jClass.getDeclaredMethods().forEach(m -> {
                    if (!m.isAbstract() || m.isNative()) {
                        solver.addEntryPoint(new EntryPoint(m, EmptyParamProvider.get()));
                        for (Var returnVar : m.getIR().getReturnVars()) {
                            csManager.getCSVarsOf(returnVar).forEach(csReturnVar -> {
                                solver.addPFGEdge(new AndroidTransferEdge(csReturnVar, csArg));
                                for (StoreArray store : returnVar.getStoreArrays()) {
                                    Var rvalue = store.getRValue();
                                    CSVar from = csManager.getCSVar(context, rvalue);
                                    solver.addPFGEdge(new AndroidTransferEdge(from, csArg));
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    @InvokeHandler(signature = {
            "<android.widget.TextView: java.lang.CharSequence getHint()>",
            "<android.content.Intent: android.os.Bundle getExtras()>"
    }, argIndexes = {BASE})
    public void getSingleMap(Context context, Invoke invoke, PointsToSet baseObjs) {
        Var result = invoke.getResult();
        if (result == null) {
            return;
        }
        CSVar csResult = csManager.getCSVar(context, result);
        baseObjs.forEach(baseObj -> getSingleMap.put(baseObj, csResult));
    }

    @InvokeHandler(signature = {
            "<android.widget.TextView: void setHint(java.lang.CharSequence)>",
            "<android.content.Intent: android.content.Intent putExtras(android.os.Bundle)>"
    }, argIndexes = {BASE})
    public void putSingleMap(Context context, Invoke invoke, PointsToSet baseObjs) {
        CSVar value = csManager.getCSVar(context, InvokeUtils.getVar(invoke, 0));
        baseObjs.forEach(baseObj -> singleMap.put(baseObj, value));
    }

    private void processGetSingleMap() {
        getSingleMap.forEach((baseObj, csResult) ->
                singleMap.get(baseObj).forEach(v -> solver.addPFGEdge(new AndroidTransferEdge(v, csResult)))
        );
    }

    @InvokeHandler(signature = {
            "<android.os.Handler: boolean postDelayed(java.lang.Runnable,long)>",
            "<android.os.Handler: boolean post(java.lang.Runnable)>"
    }, argIndexes = {0})
    public void handlerPostDelayed(Context context, Invoke invoke, PointsToSet runnableObjs) {
        runnableObjs.forEach(csObj -> {
            JMethod dispatch = hierarchy.dispatch(csObj.getObject().getType(), RUNNABLE_RUN.getRef());
            if (dispatch != null) {
                addEntryPoint(dispatch, csObj.getObject());
            }
        });
    }

}
