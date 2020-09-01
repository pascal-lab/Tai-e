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

package panda.pta.core.solver;

import panda.callgraph.AbstractCallGraph;
import panda.callgraph.Edge;
import panda.pta.core.context.Context;
import panda.pta.core.cs.CSCallSite;
import panda.pta.core.cs.CSMethod;
import panda.pta.core.cs.CSManager;
import panda.pta.element.CallSite;
import panda.pta.element.Method;
import panda.pta.statement.Call;
import panda.pta.statement.Statement;
import panda.util.CollectionUtils;

class OnFlyCallGraph extends AbstractCallGraph<CSCallSite, CSMethod> {

    private final CSManager csManager;

    OnFlyCallGraph(CSManager csManager) {
        this.csManager = csManager;
    }

    @Override
    public void addEntryMethod(CSMethod entryMethod) {
        entryMethods.add(entryMethod);
        // Let pointer analysis explicitly call addNewMethod() of this class
    }

    void addEdge(Edge<CSCallSite, CSMethod> edge) {
        CollectionUtils.addToMapSet(callSiteToEdges, edge.getCallSite(), edge);
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
                    CSCallSite csCallSite = csManager
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
