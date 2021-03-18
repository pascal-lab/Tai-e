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

package pascal.taie.analysis.oldpta.core.ci;

import pascal.taie.analysis.callgraph.AbstractCallGraph;
import pascal.taie.analysis.callgraph.Edge;
import pascal.taie.language.classes.JMethod;
import pascal.taie.analysis.oldpta.ir.Call;
import pascal.taie.analysis.oldpta.ir.CallSite;
import pascal.taie.analysis.oldpta.ir.Statement;
import pascal.taie.util.CollectionUtils;

class OnFlyCallGraph extends AbstractCallGraph<CallSite, JMethod> {

    void addEdge(Edge<CallSite, JMethod> edge) {
        CollectionUtils.addToMapSet(callSiteToEdges, edge.getCallSite(), edge);
        CollectionUtils.addToMapSet(calleeToEdges, edge.getCallee(), edge);
    }

    boolean containsEdge(Edge<CallSite, JMethod> edge) {
        return getEdgesOf(edge.getCallSite()).contains(edge);
    }

    @Override
    protected boolean addNewMethod(JMethod method) {
        if (reachableMethods.add(method)) {
            for (Statement s : method.getPTAIR().getStatements()) {
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
