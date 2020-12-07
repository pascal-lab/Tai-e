/*
 * Panda - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Panda is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.pta.core.ci;

import pascal.taie.callgraph.AbstractCallGraph;
import pascal.taie.callgraph.Edge;
import pascal.taie.pta.element.CallSite;
import pascal.taie.pta.element.Method;
import pascal.taie.pta.statement.Call;
import pascal.taie.pta.statement.Statement;
import pascal.taie.util.CollectionUtils;

class OnFlyCallGraph extends AbstractCallGraph<CallSite, Method> {

    void addEdge(Edge<CallSite, Method> edge) {
        CollectionUtils.addToMapSet(callSiteToEdges, edge.getCallSite(), edge);
        CollectionUtils.addToMapSet(calleeToEdges, edge.getCallee(), edge);
    }

    boolean containsEdge(Edge<CallSite, Method> edge) {
        return getEdgesOf(edge.getCallSite()).contains(edge);
    }

    @Override
    protected boolean addNewMethod(Method method) {
        if (reachableMethods.add(method)) {
            for (Statement s : method.getStatements()) {
                if (s instanceof Call) {
                    CallSite callSite = ((Call) s).getCallSite();
                    callSiteToContainer.put(callSite, method);
                    CollectionUtils.addToMapSet(callSitesIn, method, callSite);
                }
            }
            return true;
        } else {
            return false;
        }
    }
}
