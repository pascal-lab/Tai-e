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

package pascal.taie.oldpta.core.solver;

import pascal.taie.callgraph.CallGraph;
import pascal.taie.callgraph.Edge;
import pascal.taie.java.classes.JMethod;
import pascal.taie.oldpta.core.context.Context;
import pascal.taie.oldpta.core.cs.CSCallSite;
import pascal.taie.oldpta.core.cs.CSManager;
import pascal.taie.oldpta.core.cs.CSMethod;
import pascal.taie.oldpta.ir.Call;
import pascal.taie.oldpta.ir.CallSite;
import pascal.taie.oldpta.ir.Statement;
import pascal.taie.util.CollectionView;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static pascal.taie.util.CollectionUtils.newHybridSet;
import static pascal.taie.util.CollectionUtils.newSet;

class OnFlyCallGraph implements CallGraph<CSCallSite, CSMethod> {

    private final CSManager csManager;
    private final Set<CSMethod> entryMethods = newHybridSet();
    private final Set<CSMethod> reachableMethods = newSet();

    OnFlyCallGraph(CSManager csManager) {
        this.csManager = csManager;
    }

    void addEntryMethod(CSMethod entryMethod) {
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

    boolean addNewMethod(CSMethod csMethod) {
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
    public Stream<Edge<CSCallSite, CSMethod>> getAllEdges() {
        return reachableMethods.stream()
                .map(this::getCallSitesIn)
                .flatMap(Collection::stream)
                .map(this::getEdgesOf)
                .flatMap(Collection::stream);
    }

    @Override
    public Collection<CSMethod> getReachableMethods() {
        return reachableMethods;
    }

    @Override
    public boolean contains(CSMethod csMethod) {
        return reachableMethods.contains(csMethod);
    }

    @Nonnull
    @Override
    public Iterator<Edge<CSCallSite, CSMethod>> iterator() {
        return getAllEdges().iterator();
    }
}
