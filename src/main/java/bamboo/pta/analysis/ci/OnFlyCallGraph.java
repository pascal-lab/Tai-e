/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.pta.analysis.ci;

import bamboo.callgraph.AbstractCallGraph;
import bamboo.callgraph.Edge;
import bamboo.pta.element.CallSite;
import bamboo.pta.element.Method;
import bamboo.pta.statement.Call;
import bamboo.pta.statement.Statement;
import bamboo.util.CollectionUtils;

class OnFlyCallGraph extends AbstractCallGraph<CallSite, Method> {

    boolean addEdge(Edge<CallSite, Method> edge) {
        return CollectionUtils.addToMapSet(callSiteToEdges, edge.getCallSite(), edge) ||
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
