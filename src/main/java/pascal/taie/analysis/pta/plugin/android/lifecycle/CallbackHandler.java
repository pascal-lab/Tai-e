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

package pascal.taie.analysis.pta.plugin.android.lifecycle;

import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import java.util.Set;

public class CallbackHandler extends LifecycleHandler {

    /**
     * Records callback methods associated with argument variables whose
     * corresponding formal parameter types are Android callback types.
     *
     * <p>When objects flow to such an argument variable, the corresponding
     * callback methods may be dispatched on the object's runtime class and
     * added as entry points.
     */
    private final MultiMap<Var, JMethod> callbackMethodsByArg = Maps.newMultiMap();

    public CallbackHandler(LifecycleContext context) {
        super(context);
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        Var var = csVar.getVar();

        if (!callbackMethodsByArg.containsKey(var)) {
            return;
        }

        pts.forEach(obj -> addCallbacksForObject(
                obj.getObject(),
                callbackMethodsByArg.get(var)
        ));
    }

    @Override
    public void onNewStmt(Stmt stmt, JMethod container) {
        if (stmt instanceof Invoke invoke && !invoke.isDynamic()) {
            recordCallbackArguments(invoke);
        }
    }

    private void recordCallbackArguments(Invoke invoke) {
        MethodRef methodRef = invoke.getMethodRef();

        if (methodRef.resolveNullable() == null) {
            return;
        }

        InvokeExp invokeExp = invoke.getInvokeExp();
        MultiMap<Type, JMethod> androidCallbacks =
                handlerContext.apkInfo().androidCallbacks();

        for (int i = 0; i < invokeExp.getArgCount(); ++i) {
            Var arg = invokeExp.getArg(i);
            Type paramType = methodRef.getParameterTypes().get(i);

            if (androidCallbacks.containsKey(paramType)) {
                callbackMethodsByArg.putAll(arg, androidCallbacks.get(paramType));
            }
        }
    }

    private void addCallbacksForObject(
            Obj object,
            Set<JMethod> callbackMethods) {
        if (object.getType() instanceof ClassType classType) {
            JClass callbackClass = classType.getJClass();
            callbackMethods.forEach(callbackMethod ->
                    addCallback(callbackClass, callbackMethod, object));
        }
    }

    private void addCallback(
            JClass callbackClass,
            JMethod callbackMethod,
            Obj thisObj) {
        JMethod dispatchedCallback =
                hierarchy.dispatch(callbackClass, callbackMethod.getRef());

        if (dispatchedCallback != null) {
            addEntryPoint(dispatchedCallback, thisObj);
        }
    }
}
