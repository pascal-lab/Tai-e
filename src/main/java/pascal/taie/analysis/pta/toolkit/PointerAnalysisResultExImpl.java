/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.analysis.pta.toolkit;

import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import java.util.Set;

import static java.util.function.Predicate.not;

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
     * Map from receiver objects to the methods invoked on them.
     */
    private MultiMap<Obj, JMethod> recv2Methods;

    @Override
    public Set<JMethod> getMethodsInvokedOn(Obj obj) {
        computeMethodReceiverObjects();
        return recv2Methods.get(obj);
    }

    /**
     * Map from methods to their receiver objects.
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
            base.getCallGraph().reachableMethods()
                    .filter(not(JMethod::isStatic))
                    .forEach(method -> {
                        Var thisVar = method.getIR().getThis();
                        base.getPointsToSet(thisVar).forEach(recv -> {
                            recv2Methods.put(recv, method);
                            method2Recvs.put(method, recv);
                        });
                    });
        }
    }

    /**
     * Map from methods to the objects allocated in them.
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
}
