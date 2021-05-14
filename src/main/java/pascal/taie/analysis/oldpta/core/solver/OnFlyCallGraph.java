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

package pascal.taie.analysis.oldpta.core.solver;

import pascal.taie.analysis.graph.callgraph.AbstractCallGraph;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.oldpta.core.context.Context;
import pascal.taie.analysis.oldpta.core.cs.CSCallSite;
import pascal.taie.analysis.oldpta.core.cs.CSManager;
import pascal.taie.analysis.oldpta.core.cs.CSMethod;
import pascal.taie.analysis.oldpta.ir.Call;
import pascal.taie.analysis.oldpta.ir.CallSite;
import pascal.taie.analysis.oldpta.ir.Statement;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.CollectionView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static pascal.taie.util.collection.SetUtils.newHybridSet;
import static pascal.taie.util.collection.SetUtils.newSet;

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
        for (Statement s : method.getPTAIR().getStatements()) {
            if (s instanceof Call) {
                CallSite callSite = ((Call) s).getCallSite();
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

    @Override
    public Stream<CSMethod> reachableMethods() {
        return reachableMethods.stream();
    }

    @Override
    public boolean contains(CSMethod csMethod) {
        return reachableMethods.contains(csMethod);
    }
}
