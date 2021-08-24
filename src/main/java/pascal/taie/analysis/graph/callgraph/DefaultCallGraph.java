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

package pascal.taie.analysis.graph.callgraph;

import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;

/**
 * Default implementation of call graph.
 */
public class DefaultCallGraph extends AbstractCallGraph<Invoke, JMethod> {

    /**
     * Adds an entry method to this call graph.
     */
    public void addEntryMethod(JMethod entryMethod) {
        entryMethods.add(entryMethod);
    }

    /**
     * Adds a reachable method to this call graph.
     *
     * @return true if this call graph changed as a result of the call,
     * otherwise false.
     */
    public boolean addReachableMethod(JMethod method) {
        if (reachableMethods.add(method)) {
            if (!method.isAbstract()) {
                method.getIR().forEach(stmt -> {
                    if (stmt instanceof Invoke) {
                        Invoke invoke = (Invoke) stmt;
                        callSiteToContainer.put(invoke, method);
                        Maps.addToMapSet(callSitesIn, method, invoke);
                    }
                });
            }
            return true;
        }
        return false;
    }

    /**
     * Adds a new call graph edge to this call graph.
     *
     * @param edge the call edge to be added
     * @return true if the call graph changed as a result of the call,
     * otherwise false.
     */
    public boolean addEdge(Edge<Invoke, JMethod> edge) {
        if (Maps.addToMapSet(callSiteToEdges, edge.getCallSite(), edge)) {
            Maps.addToMapSet(calleeToEdges, edge.getCallee(), edge);
            return true;
        } else {
            return false;
        }
    }
}
