/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.analysis.pta;

import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;

import java.util.Map;
import java.util.Set;

import static java.util.function.Predicate.not;

public class PointerAnalysisResultExImpl implements PointerAnalysisResultEx {

    private final PointerAnalysisResult pta;

    public PointerAnalysisResultExImpl(PointerAnalysisResult pta) {
        this.pta = pta;
    }

    @Override
    public PointerAnalysisResult getPointerAnalysisResult() {
        return pta;
    }

    /**
     * Map from receiver objects to the methods invoked on them.
     */
    private Map<Obj, Set<JMethod>> recv2Methods;

    @Override
    public Set<JMethod> getMethodsInvokedOn(Obj obj) {
        computeMethodReceiverObjects();
        return recv2Methods.getOrDefault(obj, Set.of());
    }

    /**
     * Map from methods to their receiver objects.
     */
    private Map<JMethod, Set<Obj>> method2Recvs;

    @Override
    public Set<Obj> getReceiverObjectsOf(JMethod method) {
        computeMethodReceiverObjects();
        return method2Recvs.getOrDefault(method, Set.of());
    }

    private void computeMethodReceiverObjects() {
        if (recv2Methods == null && method2Recvs == null) {
            recv2Methods = Maps.newMap();
            method2Recvs = Maps.newMap();
            pta.getCallGraph().reachableMethods()
                    .filter(not(JMethod::isStatic))
                    .forEach(method -> {
                        Var thisVar = method.getIR().getThis();
                        pta.getPointsToSet(thisVar).forEach(recv -> {
                            Maps.addToMapSet(recv2Methods, recv, method);
                            Maps.addToMapSet(method2Recvs, method, recv);
                        });
                    });
        }
    }
}
