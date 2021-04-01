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

package pascal.taie.analysis.pta.core.solver;

import pascal.taie.analysis.graph.callgraph.AbstractCallGraph;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.CollectionView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static pascal.taie.util.collection.CollectionUtils.newHybridSet;
import static pascal.taie.util.collection.CollectionUtils.newSet;

class OnFlyCallGraph extends AbstractCallGraph<CSCallSite, CSMethod> {

    private final CSManager csManager;

    OnFlyCallGraph(CSManager csManager) {
        this.csManager = csManager;
        this.entryMethods = newHybridSet();
        this.reachableMethods = newSet();
    }

    @Override
    public void addEntryMethod(CSMethod entryMethod) {
        entryMethods.add(entryMethod);
        // Let pointer analysis explicitly call addNewMethod() of this class
    }

    @Override
    public Collection<CSMethod> getEntryMethods() {
        return entryMethods;
    }

    void addEdge(Edge<CSCallSite, CSMethod> edge) {
        edge.getCallSite().addEdge(edge);
        edge.getCallee().addCaller(edge.getCallSite());
    }

    boolean containsEdge(Edge<CSCallSite, CSMethod> edge) {
        return getEdgesOf(edge.getCallSite()).contains(edge);
    }

    @Override
    public boolean addNewMethod(CSMethod csMethod) {
        if (reachableMethods.add(csMethod)) {
            getCallSitesIn(csMethod).forEach(csCallSite ->
                    csCallSite.setContainer(csMethod));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Collection<CSMethod> getCallees(CSCallSite csCallSite) {
        return CollectionView.of(csCallSite.getEdges(), Edge::getCallee);
    }

    @Override
    public Collection<CSCallSite> getCallers(CSMethod callee) {
        return callee.getCallers();
    }

    @Override
    public CSMethod getContainerMethodOf(CSCallSite csCallSite) {
        return csCallSite.getContainer();
    }

    @Override
    public Collection<CSCallSite> getCallSitesIn(CSMethod csMethod) {
        JMethod method = csMethod.getMethod();
        Context context = csMethod.getContext();
        List<CSCallSite> callSites = new ArrayList<>();
        for (Stmt s : method.getIR().getStmts()) {
            if (s instanceof Invoke) {
                InvokeExp callSite = ((Invoke) s).getInvokeExp();
                CSCallSite csCallSite = csManager
                        .getCSCallSite(context, callSite);
                callSites.add(csCallSite);
            }
        }
        return callSites;
    }

    @Override
    public Collection<Edge<CSCallSite, CSMethod>> getEdgesOf(CSCallSite csCallSite) {
        return csCallSite.getEdges();
    }

    @Override
    public Stream<Edge<CSCallSite, CSMethod>> edges() {
        return reachableMethods.stream()
                .map(this::getCallSitesIn)
                .flatMap(Collection::stream)
                .map(this::getEdgesOf)
                .flatMap(Collection::stream);
    }
}
