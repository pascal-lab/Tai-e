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
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.NewObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.EmptyParamProvider;
import pascal.taie.analysis.pta.core.solver.EntryPoint;
import pascal.taie.analysis.pta.plugin.android.AndroidModelEdge;
import pascal.taie.analysis.pta.plugin.util.CSObjs;
import pascal.taie.analysis.pta.plugin.util.InvokeHandler;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.android.AndroidClassNames;
import pascal.taie.ir.exp.CastExp;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.StringLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import java.util.List;

import static pascal.taie.analysis.pta.plugin.util.InvokeUtils.BASE;

/**
 * Models assorted Android framework behaviors that do not fit the dedicated
 * lifecycle, ICC, or map-like handlers.
 */
public class OtherMiscModel extends AndroidMiscHandler {

    private static final List<String> CAST_PASSTHROUGH_TYPES = List.of(
            AndroidClassNames.VIEW,
            AndroidClassNames.TEXT_VIEW,
            AndroidClassNames.URL_CONNECTION
    );

    private static final String ACCOUNT_MANAGER_GET_ACCOUNTS =
            "<android.accounts.AccountManager: android.accounts.Account[] getAccounts()>";

    /**
     * Casts whose source value should be conservatively propagated to the cast result.
     */
    private final MultiMap<CSVar, CSVar> castPassthroughs = Maps.newMultiMap();

    public OtherMiscModel(AndroidMiscContext context) {
        super(context);
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        Context context = csMethod.getContext();
        csMethod.getMethod().getIR().getStmts().forEach(stmt -> processStmt(context, stmt));
    }

    private void processStmt(Context context, Stmt stmt) {
        if (stmt instanceof Cast cast) {
            recordCastPassthrough(context, cast);
        } else if (stmt instanceof LoadField loadField) {
            modelAccountFieldRead(context, loadField);
        } else if (stmt instanceof Invoke invoke && !invoke.isDynamic()) {
            modelAccountManagerGetAccounts(context, invoke);
        }
    }

    /**
     * Some framework code casts between Android/JRE wrapper types before reading values from them.
     * Propagating the source value to the cast result keeps these casts from breaking data flow.
     */
    private void recordCastPassthrough(Context context, Cast castStmt) {
        CastExp cast = castStmt.getRValue();
        String valueType = cast.getValue().getType().getName();
        if (CAST_PASSTHROUGH_TYPES.contains(valueType)) {
            CSVar from = csManager.getCSVar(context, cast.getValue());
            CSVar to = csManager.getCSVar(context, castStmt.getLValue());
            castPassthroughs.put(from, to);
        }
    }

    /**
     * Account.name/type are frequently used as string-like identifiers.
     *
     * Conservatively connect the Account object itself to field-read results
     * to avoid missing flows where account identity information is propagated
     * through Account.name or Account.type.
     *
     * This may introduce over-approximation. More precise handling can be
     * refined later via taint configurations, e.g., customized transfer rules
     * or sink modeling.
     */
    private void modelAccountFieldRead(Context context, LoadField loadField) {
        if (loadField.getFieldAccess() instanceof InstanceFieldAccess access) {
            JField field = loadField.getFieldRef().resolveNullable();
            if (field != null && field.getDeclaringClass().getName().equals(AndroidClassNames.ACCOUNT)) {
                solver.addPFGEdge(new AndroidModelEdge(
                        csManager.getCSVar(context, access.getBase()),
                        csManager.getCSVar(context, loadField.getLValue()))
                );
            }
        }
    }

    private void modelAccountManagerGetAccounts(Context context, Invoke invoke) {
        JMethod method = invoke.getMethodRef().resolveNullable();
        if (method != null && method.getSignature().equals(ACCOUNT_MANAGER_GET_ACCOUNTS)) {
            addResultObjectForInvoke(context, invoke);
        }
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pointsToSet) {
        super.onNewPointsToSet(csVar, pointsToSet);
        castPassthroughs.get(csVar).forEach(to -> solver.addPointsTo(to, pointsToSet));
    }

    // Preserve the concrete class-name string for ICC patterns such as
    // XXXActivity.class.getName().
    @InvokeHandler(signature = "<java.lang.Class: java.lang.String getName()>", argIndexes = {BASE})
    public void classGetName(Context context, Invoke invoke, PointsToSet classes) {
        Var result = invoke.getResult();
        if (result != null) {
            classes.forEach(csObj -> {
                if (csObj.getObject().getAllocation() instanceof ClassLiteral classLiteral) {
                    Obj className = handlerContext.androidObjManager()
                            .mockObjByString(StringLiteral.get(classLiteral.getTypeValue().getName()), result);
                    solver.addVarPointsTo(context, result, className);
                }
            });
        }
    }

    @InvokeHandler(signature = "<java.lang.reflect.Method: java.lang.String getName()>", argIndexes = {BASE})
    public void methodGetName(Context context, Invoke invoke, PointsToSet classes) {
        Var result = invoke.getResult();
        if (result != null) {
            classes.forEach(csObj -> {
                JMethod method = CSObjs.toMethod(csObj);
                if (method != null) {
                    Obj methodName = handlerContext.androidObjManager()
                            .mockObjByString(StringLiteral.get(method.getName()), result);
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

    /**
     * Models {@code Intent.getExtras()} by propagating the whole Bundle object.
     */
    @InvokeHandler(signature = {
            "<android.content.Intent: android.os.Bundle getExtras()>"},
            argIndexes = {BASE})
    public void intentGetExtras(Context context, Invoke invoke, PointsToSet mapObjs) {
        Var result = invoke.getResult();
        if (result != null) {
            mapObjs.forEach(mapObj ->
                    solver.addVarPointsTo(context, result, mapObj));
        }
    }

    /**
     * Methods exposed through addJavascriptInterface may be called by JavaScript at runtime.
     * Treat them as entry points and connect their return values back to the interface object,
     * preserving the previous conservative behavior for array-store flows from return variables.
     */
    @InvokeHandler(signature = {
            "<android.webkit.WebView: void addJavascriptInterface(java.lang.Object,java.lang.String)>"
    }, argIndexes = {0})
    public void addJavascriptInterface(Context context, Invoke invoke, PointsToSet argObjs) {
        CSVar interfaceVar = csManager.getCSVar(context, InvokeUtils.getVar(invoke, 0));
        argObjs.forEach(argObj -> {
            if (argObj.getObject() instanceof NewObj newObj
                    && newObj.getType() instanceof ClassType classType) {
                JClass jClass = classType.getJClass();
                jClass.getDeclaredMethods().forEach(m -> {
                    if (!m.isAbstract() || m.isNative()) {
                        solver.addEntryPoint(new EntryPoint(m, EmptyParamProvider.get()));
                        for (Var returnVar : m.getIR().getReturnVars()) {
                            csManager.getCSVarsOf(returnVar).forEach(csReturnVar -> {
                                solver.addPFGEdge(new AndroidModelEdge(csReturnVar, interfaceVar));
                                addArrayStoreFlowsFromReturnVar(context, interfaceVar, returnVar);
                            });
                        }
                    }
                });
            }
        });
    }

    private void addArrayStoreFlowsFromReturnVar(Context context, CSVar interfaceVar, Var returnVar) {
        returnVar.getStoreArrays().forEach(store -> {
            CSVar from = csManager.getCSVar(context, store.getRValue());
            solver.addPFGEdge(new AndroidModelEdge(from, interfaceVar));
        });
    }

}
