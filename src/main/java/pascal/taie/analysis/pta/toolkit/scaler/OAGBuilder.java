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

package pascal.taie.analysis.pta.toolkit.scaler;

import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.toolkit.PointerAnalysisResultEx;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.graph.Graph;
import pascal.taie.util.graph.SimpleGraph;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

class OAGBuilder {

    /**
     * Builds object allocation graph.
     *
     * @return the object allocation graph for the program.
     */
    static Graph<Obj> build(PointerAnalysisResultEx pta) {
        SimpleGraph<Obj> oag = new SimpleGraph<>();
        computeInvokedMethods(pta).forEach((obj, methods) -> methods.stream()
                .map(pta::getObjectsAllocatedIn)
                .flatMap(Set::stream)
                .forEach(succ -> oag.addEdge(obj, succ)));
        return oag;
    }

    /**
     * Computes the methods invoked on all objects in the program.
     * For a static method, say m, we trace back its caller chain until
     * we find the first instance method on the chain, say m' , and
     * consider that m is invoked on the receiver object of m'.
     */
    private static Map<Obj, Set<JMethod>> computeInvokedMethods(
            PointerAnalysisResultEx pta) {
        Map<Obj, Set<JMethod>> invokedMethods = Maps.newMap();
        pta.getBase().getObjects().forEach(obj -> {
            Set<JMethod> methods = Sets.newHybridSet();
            Queue<JMethod> queue = new ArrayDeque<>(
                    pta.getMethodsInvokedOn(obj));
            while (!queue.isEmpty()) {
                JMethod method = queue.poll();
                methods.add(method);
                pta.getBase().getCallGraph()
                        .getCalleesOfM(method)
                        .stream()
                        .filter(m -> m.isStatic() && !methods.contains(m))
                        .forEach(queue::add);
            }
            invokedMethods.put(obj, methods);
        });
        return invokedMethods;
    }
}
