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

package bamboo.pta.analysis.solver;

import bamboo.callgraph.AbstractCallGraph;
import bamboo.callgraph.Edge;
import bamboo.pta.analysis.context.Context;
import bamboo.pta.analysis.data.CSCallSite;
import bamboo.pta.analysis.data.CSMethod;
import bamboo.pta.analysis.data.DataManager;
import bamboo.pta.element.CallSite;
import bamboo.pta.element.Method;
import bamboo.pta.statement.Call;
import bamboo.pta.statement.Statement;
import bamboo.util.CollectionUtils;

class OnFlyCallGraph extends AbstractCallGraph<CSCallSite, CSMethod> {

    private DataManager dataManager;

    OnFlyCallGraph(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    boolean addEdge(Edge<CSCallSite, CSMethod> edge) {
        return CollectionUtils.addToMapSet(callSiteToEdges, edge.getCallSite(), edge) ||
                CollectionUtils.addToMapSet(calleeToEdges, edge.getCallee(), edge);
    }

    boolean containsEdge(Edge<CSCallSite, CSMethod> edge) {
        return getEdgesOf(edge.getCallSite()).contains(edge);
    }

    @Override
    protected boolean addNewMethod(CSMethod csMethod) {
        if (reachableMethods.add(csMethod)) {
            Method method = csMethod.getMethod();
            Context context = csMethod.getContext();
            for (Statement s : method.getStatements()) {
                if (s instanceof Call) {
                    CallSite callSite = ((Call) s).getCallSite();
                    CSCallSite csCallSite = dataManager
                            .getCSCallSite(context, callSite);
                    callSiteToContainer.put(csCallSite, csMethod);
                    CollectionUtils.addToMapSet(callSitesIn, csMethod, csCallSite);
                }
            }
            return true;
        } else {
            return false;
        }
    }
}
