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
import pascal.taie.util.collection.MapUtils;

/**
 * Default implementation of call graph.
 */
public class DefaultCallGraph extends AbstractCallGraph<Invoke, JMethod> {

    /**
     * Adds an entry method to this call graph.
     */
    public void addEntryMethod(JMethod entryMethod) {
        entryMethods.add(entryMethod);
        addNewMethod(entryMethod);
    }

    /**
     * Adds a new method to this call graph.
     */
    public void addNewMethod(JMethod method) {
        if (reachableMethods.add(method)) {
            if (!method.isAbstract()) {
                method.getIR().getStmts().forEach(stmt -> {
                    if (stmt instanceof Invoke) {
                        Invoke invoke = (Invoke) stmt;
                        callSiteToContainer.put(invoke, method);
                        MapUtils.addToMapSet(callSitesIn, method, invoke);
                    }
                });
            }
        }
    }

    /**
     * Adds a new call graph edge to this call graph.
     *
     * @param edge the call edge to be added
     * @return true if the new-added edge is absent in this call graph before
     */
    public boolean addEdge(Edge<Invoke, JMethod> edge) {
        if (MapUtils.addToMapSet(callSiteToEdges, edge.getCallSite(), edge)) {
            MapUtils.addToMapSet(calleeToEdges, edge.getCallee(), edge);
            addNewMethod(edge.getCallee());
            return true;
        } else {
            return false;
        }
    }
}
