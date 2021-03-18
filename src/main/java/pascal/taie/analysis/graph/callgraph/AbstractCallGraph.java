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

package pascal.taie.analysis.graph.callgraph;

import pascal.taie.util.CollectionUtils;
import pascal.taie.util.CollectionView;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static pascal.taie.util.CollectionUtils.newMap;
import static pascal.taie.util.CollectionUtils.newSet;

public abstract class AbstractCallGraph<CallSite, Method>
        implements CallGraph<CallSite, Method> {

    protected final Map<CallSite, Set<Edge<CallSite, Method>>> callSiteToEdges;
    protected final Map<Method, Set<Edge<CallSite, Method>>> calleeToEdges;
    protected final Map<CallSite, Method> callSiteToContainer;
    protected final Map<Method, Set<CallSite>> callSitesIn;
    protected final Set<Method> entryMethods;
    protected final Set<Method> reachableMethods;

    protected AbstractCallGraph() {
        callSiteToEdges = newMap();
        calleeToEdges = newMap();
        callSiteToContainer = newMap();
        callSitesIn = newMap();
        entryMethods = newSet();
        reachableMethods = newSet();
    }

    public void addEntryMethod(Method entryMethod) {
        entryMethods.add(entryMethod);
        addNewMethod(entryMethod);
    }

    public void addEdge(CallSite callSite, Method callee, CallKind kind) {
        addNewMethod(callee);
        Edge<CallSite, Method> edge = new Edge<>(kind, callSite, callee);
        CollectionUtils.addToMapSet(callSiteToEdges, callSite, edge);
        CollectionUtils.addToMapSet(calleeToEdges, callee, edge);
    }

    /**
     * Adds a new method to this call graph.
     * Returns true if the method was not in this call graph.
     */
    protected abstract boolean addNewMethod(Method method);

    @Override
    public Collection<Method> getCallees(CallSite callSite) {
        Set<Edge<CallSite, Method>> edges = callSiteToEdges.get(callSite);
        if (edges != null) {
            return CollectionView.of(edges, Edge::getCallee);
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public Collection<CallSite> getCallers(Method callee) {
        Set<Edge<CallSite, Method>> edges = calleeToEdges.get(callee);
        if (edges != null) {
            return CollectionView.of(edges, Edge::getCallSite);
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public Method getContainerMethodOf(CallSite callSite) {
        return callSiteToContainer.get(callSite);
    }

    @Override
    public Collection<CallSite> getCallSitesIn(Method method) {
        return callSitesIn.getOrDefault(method, Collections.emptySet());
    }

    @Override
    public Collection<Edge<CallSite, Method>> getEdgesOf(CallSite callSite) {
        return callSiteToEdges.getOrDefault(callSite, Collections.emptySet());
    }

    @Override
    public Stream<Edge<CallSite, Method>> getAllEdges() {
        return callSiteToEdges.values()
                .stream()
                .flatMap(Set::stream);
    }

    @Override
    public Collection<Method> getEntryMethods() {
        return entryMethods;
    }

    @Override
    public Collection<Method> getReachableMethods() {
        return reachableMethods;
    }

    @Override
    public boolean contains(Method method) {
        return reachableMethods.contains(method);
    }

    @Nonnull
    @Override
    public Iterator<Edge<CallSite, Method>> iterator() {
        return getAllEdges().iterator();
    }
}
