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
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Pair;

import java.util.stream.IntStream;

public class CallbackHandler extends LifecycleHandler {

    private final MultiMap<Var, JMethod> callbackMethods = Maps.newMultiMap();

    public CallbackHandler(LifecycleContext context) {
        super(context);
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        Var var = csVar.getVar();
        pts.forEach(thisObj -> {
            if (thisObj.getObject().getType() instanceof ClassType classType) {
                callbackMethods.get(var).forEach(callbackMethod ->
                        addCallback(classType.getJClass(), callbackMethod, thisObj.getObject()));
            }
        });
    }

    @Override
    public void onNewStmt(Stmt stmt, JMethod container) {
        if (stmt instanceof Invoke invoke && !invoke.isDynamic()) {
            processCallback(invoke);
        }
    }

    private void addCallback(JClass callbackClass, JMethod callbackMethod, Obj thisObj) {
        JMethod callback = hierarchy.dispatch(callbackClass, callbackMethod.getRef());
        if (callback != null) {
            addEntryPoint(callback, thisObj);
        }
    }

    private void processCallback(Invoke invoke) {
        MethodRef methodRef = invoke.getMethodRef();
        JMethod resolve = methodRef.resolveNullable();
        if (resolve != null) {
            InvokeExp invokeExp = invoke.getInvokeExp();
            IntStream.range(0, invokeExp.getArgCount())
                    .mapToObj(i -> new Pair<>(invokeExp.getArg(i), methodRef.getParameterTypes().get(i)))
                    .filter(pair -> handlerContext.apkInfo().androidCallbacks().containsKey(pair.second()))
                    .forEach(pair -> callbackMethods.putAll(pair.first(), handlerContext.apkInfo().androidCallbacks().get(pair.second())));
        }
    }

}
