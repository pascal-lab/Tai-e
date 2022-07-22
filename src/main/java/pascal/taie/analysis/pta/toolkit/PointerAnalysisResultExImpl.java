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

    /**
     * Constructs an extended pointer analysis result.
     *
     * @param base      base pointer analysis result
     * @param eagerInit whether initialize all fields eagerly; if this result
     *                  will be accessed in concurrent setting, then the caller
     *                  should give {@code true}.
     */
    public PointerAnalysisResultExImpl(
            PointerAnalysisResult base, boolean eagerInit) {
        this.base = base;
        if (eagerInit) {
            initMethodReceiverObjects();
            initAllocatedObjects();
            initType2Objects();
        }
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
        MultiMap<Obj, JMethod> map = recv2Methods;
        if (map == null) {
            initMethodReceiverObjects();
            map = recv2Methods;
        }
        return map.get(obj);
    }

    /**
     * Map from each method to its receiver objects.
     */
    private MultiMap<JMethod, Obj> method2Recvs;

    @Override
    public Set<Obj> getReceiverObjectsOf(JMethod method) {
        MultiMap<JMethod, Obj> map = method2Recvs;
        if (map == null) {
            initMethodReceiverObjects();
            map = method2Recvs;
        }
        return map.get(method);
    }

    private void initMethodReceiverObjects() {
        MultiMap<Obj, JMethod> r2m = Maps.newMultiMap();
        MultiMap<JMethod, Obj> m2r = Maps.newMultiMap();
        for (JMethod method : base.getCallGraph()) {
            if (!method.isStatic()) {
                Var thisVar = method.getIR().getThis();
                for (Obj recv : base.getPointsToSet(thisVar)) {
                    r2m.put(recv, method);
                    m2r.put(method, recv);
                }
            }
        }
        recv2Methods = r2m;
        method2Recvs = m2r;
    }

    /**
     * Map from each method to the objects allocated in it.
     */
    private MultiMap<JMethod, Obj> method2Objs;

    @Override
    public Set<Obj> getObjectsAllocatedIn(JMethod method) {
        MultiMap<JMethod, Obj> map = method2Objs;
        if (map == null) {
            initAllocatedObjects();
            map = method2Objs;
        }
        return map.get(method);
    }

    private void initAllocatedObjects() {
        MultiMap<JMethod, Obj> map = Maps.newMultiMap();
        for (Obj obj : base.getObjects()) {
            obj.getContainerMethod().ifPresent(m -> map.put(m, obj));
        }
        method2Objs = map;
    }

    /**
     * Map from each type to the objects of the type.
     */
    private MultiMap<Type, Obj> type2Objs;

    @Override
    public Set<Obj> getObjectsOf(Type type) {
        MultiMap<Type, Obj> map = type2Objs;
        if (map == null) {
            initType2Objects();
            map = type2Objs;
        }
        return map.get(type);
    }

    @Override
    public Set<Type> getObjectTypes() {
        MultiMap<Type, Obj> map = type2Objs;
        if (map == null) {
            initType2Objects();
            map = type2Objs;
        }
        return map.keySet();
    }

    private void initType2Objects() {
        MultiMap<Type, Obj> map = Maps.newMultiMap();
        for (Obj obj : base.getObjects()) {
            map.put(obj.getType(), obj);
        }
        type2Objs = map;
    }
}
