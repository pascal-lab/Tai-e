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
import pascal.taie.ir.exp.StringLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;

import java.util.Set;

import static pascal.taie.analysis.pta.plugin.util.InvokeUtils.BASE;

public class OtherMiscModel extends AndroidMiscHandler {

    private final Set<CSCallSite> subStringInvokes = Sets.newSet();

    private final MultiMap<CSObj, CSVar> hintMap = Maps.newMultiMap();

    private final MultiMap<CSObj, CSVar> viewObj2GetHintRes = Maps.newMultiMap();

    private final MultiMap<CSVar, CSVar> castMap = Maps.newMultiMap();

    public OtherMiscModel(AndroidMiscContext specificContext) {
        super(specificContext);
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        Context context = csMethod.getContext();
        // propagate cast in android system class type
        csMethod.getMethod().getIR().getStmts().stream().filter(stmt -> stmt instanceof Cast)
                .map(stmt -> (Cast) stmt).forEach(stmt -> {
                    CastExp cast = stmt.getRValue();
                    if (cast.getValue().getType().getName().equals(AndroidClassNames.VIEW)) {
                        CSVar from = csManager.getCSVar(context, cast.getValue());
                        CSVar to = csManager.getCSVar(context, stmt.getLValue());
                        castMap.put(from, to);
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
        processGetHint();
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

//    @InvokeHandler(signature = "<java.lang.String: java.lang.String substring(int)>", argIndexes = {BASE})
//    public void subString(Context context, Invoke invoke, PointsToSet baseObjs) {
//        Var result = invoke.getResult();
//        Var index = invoke.getInvokeExp().getArg(0);
//        if (result == null || subStringInvokes.contains(csManager.getCSCallSite(context, invoke))) {
//            return;
//        }
//
//        baseObjs.forEach(baseObj -> {
//            if (baseObj.getObject() instanceof ConstantObj constantObj && constantObj.getAllocation() instanceof StringLiteral stringLiteral && index.isConst() && index.getConstValue() instanceof IntLiteral intLiteral) {
//                subStringInvokes.add(csManager.getCSCallSite(context, invoke));
//                try {
//                    Obj subString = handlerContext.androidObjManager().getAndroidStringObj(StringLiteral.get(stringLiteral.getString().substring(intLiteral.getValue())), result);
//                    solver.addVarPointsTo(context, result, subString);
//                } catch (Exception ignored) {
//                }
//            }
//        });
//    }

    @InvokeHandler(signature = "<java.util.List: java.lang.Object[] toArray(java.lang.Object[])>", argIndexes = {BASE, 0})
    public void toArray(Context context, Invoke invoke, PointsToSet baseObjs, PointsToSet arrayObjs) {
        Var result = invoke.getResult();
        if (result == null) {
            return;
        }
        baseObjs.forEach(baseObj -> {
            arrayObjs.getObjects().forEach(arrayObj -> {
                if (baseObj.getObject() instanceof MockObj mockObj) {
                    Obj newMockObj = heapModel.getMockObj(mockObj.getDescriptor(), mockObj.getAllocation(), arrayObj.getObject().getType(), mockObj.isFunctional());
                    solver.addVarPointsTo(context, result, newMockObj);
                }
            });
        });
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
            "<android.widget.TextView: java.lang.CharSequence getHint()>"
    }, argIndexes = {BASE})
    public void viewGetHint(Context context, Invoke invoke, PointsToSet viewObjs) {
        Var result = invoke.getResult();
        if (result == null) {
            return;
        }
        viewObjs.forEach(viewObj -> {
            CSVar csResult = csManager.getCSVar(context, result);
            viewObj2GetHintRes.put(viewObj, csResult);
        });
    }

    @InvokeHandler(signature = {
            "<android.widget.TextView: void setHint(java.lang.CharSequence)>"
    }, argIndexes = {BASE})
    public void viewSetHint(Context context, Invoke invoke, PointsToSet viewObjs) {
        viewObjs.forEach(viewObj -> hintMap.put(viewObj, csManager.getCSVar(context, InvokeUtils.getVar(invoke, 0))));
    }

    private void processGetHint() {
        viewObj2GetHintRes.forEach((viewObj, csResult) ->
                hintMap.get(viewObj).forEach(hintVar -> solver.addPFGEdge(new AndroidTransferEdge(hintVar, csResult)))
        );
    }

}
