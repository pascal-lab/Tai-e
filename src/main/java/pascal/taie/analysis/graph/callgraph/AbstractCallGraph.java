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

import pascal.taie.util.collection.CollectionView;
import pascal.taie.util.collection.MapUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static pascal.taie.util.collection.MapUtils.newMap;
import static pascal.taie.util.collection.SetUtils.newSet;

public abstract class AbstractCallGraph<CallSite, Method>
        implements CallGraph<CallSite, Method> {

    protected final Map<CallSite, Set<Edge<CallSite, Method>>> callSiteToEdges;
    protected final Map<Method, Set<Edge<CallSite, Method>>> calleeToEdges;
    protected final Map<CallSite, Method> callSiteToContainer;
    protected final Map<Method, Set<CallSite>> callSitesIn;
    protected Set<Method> entryMethods;
    protected Set<Method> reachableMethods;

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
        MapUtils.addToMapSet(callSiteToEdges, callSite, edge);
        MapUtils.addToMapSet(calleeToEdges, callee, edge);
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
    public Stream<Edge<CallSite, Method>> edges() {
        return callSiteToEdges.values()
                .stream()
                .flatMap(Set::stream);
    }

    @Override
    public int getNumberOfEdges() {
        return callSiteToEdges.values()
                .stream()
                .mapToInt(Set::size)
                .sum();
    }

    @Override
    public Collection<Method> getEntryMethods() {
        return entryMethods;
    }

    @Override
    public Stream<Method> reachableMethods() {
        return reachableMethods.stream();
    }

    @Override
    public int getNumberOfMethods() {
        return reachableMethods.size();
    }

    @Override
    public boolean contains(Method method) {
        return reachableMethods.contains(method);
    }

    // Implementation for Graph interface.
    
    @Override
    public boolean hasNode(Method node) {
        return contains(node);
    }

    @Override
    public boolean hasEdge(Method source, Method target) {
        return succsOf(source).anyMatch(target::equals);
    }

    @Override
    public Stream<Method> predsOf(Method node) {
        return getCallers(node)
                .stream()
                .map(this::getContainerMethodOf)
                .distinct();
    }

    @Override
    public Stream<Method> succsOf(Method node) {
        return getCallSitesIn(node)
                .stream()
                .map(this::getCallees)
                .flatMap(Collection::stream)
                .distinct();
    }

    @Override
    public Stream<Method> nodes() {
        return reachableMethods();
    }

    @Override
    public int getNumberOfNodes() {
        return reachableMethods.size();
    }
}
