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

package pascal.taie.analysis.pta.toolkit;

import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import java.util.Set;

public class PointerAnalysisResultExImpl implements PointerAnalysisResultEx {

    private final PointerAnalysisResult base;

    public PointerAnalysisResultExImpl(PointerAnalysisResult base) {
        this.base = base;
    }

    @Override
    public PointerAnalysisResult getBase() {
        return base;
    }

    /**
     * Map from each receiver object to the methods invoked on it.
     */
    private MultiMap<Obj, JMethod> recv2Methods;

    @Override
    public Set<JMethod> getMethodsInvokedOn(Obj obj) {
        computeMethodReceiverObjects();
        return recv2Methods.get(obj);
    }

    /**
     * Map from each method to its receiver objects.
     */
    private MultiMap<JMethod, Obj> method2Recvs;

    @Override
    public Set<Obj> getReceiverObjectsOf(JMethod method) {
        computeMethodReceiverObjects();
        return method2Recvs.get(method);
    }

    private void computeMethodReceiverObjects() {
        if (recv2Methods == null && method2Recvs == null) {
            recv2Methods = Maps.newMultiMap();
            method2Recvs = Maps.newMultiMap();
            base.getCallGraph().forEach(method -> {
                if (!method.isStatic()) {
                    Var thisVar = method.getIR().getThis();
                    base.getPointsToSet(thisVar).forEach(recv -> {
                        recv2Methods.put(recv, method);
                        method2Recvs.put(method, recv);
                    });
                }
            });
        }
    }

    /**
     * Map from each method to the objects allocated in it.
     */
    private MultiMap<JMethod, Obj> method2Objs;

    @Override
    public Set<Obj> getObjectsAllocatedIn(JMethod method) {
        computeAllocatedObjects();
        return method2Objs.get(method);
    }

    private void computeAllocatedObjects() {
        if (method2Objs == null) {
            method2Objs = Maps.newMultiMap();
            base.getObjects().forEach(obj ->
                    obj.getContainerMethod().ifPresent(m ->
                            method2Objs.put(m, obj)));
        }
    }

    /**
     * Map from each type to the objects of the type.
     */
    private MultiMap<Type, Obj> type2Objs;

    @Override
    public Set<Obj> getObjectsOf(Type type) {
        computeType2Objects();
        return type2Objs.get(type);
    }

    private void computeType2Objects() {
        if (type2Objs == null) {
            type2Objs = Maps.newMultiMap();
            base.getObjects().forEach(obj -> type2Objs.put(obj.getType(), obj));
        }
    }
}
