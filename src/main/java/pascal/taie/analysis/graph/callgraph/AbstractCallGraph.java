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

import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.Views;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Common functionality for {@link CallGraph} implementations.
 * This class contains the data structures and methods for storing and
 * accessing information of a call graph. The logic of modifying
 * (adding new call edges and methods) is left to its subclasses.
 *
 * @param <CallSite> type of call sites
 * @param <Method>   type of methods
 */
public abstract class AbstractCallGraph<CallSite, Method>
        implements CallGraph<CallSite, Method> {

    protected final MultiMap<CallSite, Edge<CallSite, Method>> callSiteToEdges = Maps.newMultiMap();
    protected final MultiMap<Method, Edge<CallSite, Method>> calleeToEdges = Maps.newMultiMap();
    protected final Map<CallSite, Method> callSiteToContainer = Maps.newMap();
    protected final MultiMap<Method, CallSite> callSitesIn = Maps.newMultiMap(Sets::newHybridOrderedSet);
    protected final Set<Method> entryMethods = Sets.newSet();
    protected final Set<Method> reachableMethods = Sets.newSet();

    @Override
    public Set<CallSite> getCallersOf(Method callee) {
        return Views.toMappedSet(calleeToEdges.get(callee), Edge::getCallSite);
    }

    @Override
    public Set<Method> getCalleesOf(CallSite callSite) {
        return Views.toMappedSet(callSiteToEdges.get(callSite), Edge::getCallee);
    }

    @Override
    public Set<Method> getCalleesOfM(Method caller) {
        return callSitesIn(caller)
                .flatMap(cs -> getCalleesOf(cs).stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Method getContainerOf(CallSite callSite) {
        return callSiteToContainer.get(callSite);
    }

    @Override
    public Set<CallSite> getCallSitesIn(Method method) {
        return callSitesIn.get(method);
    }

    @Override
    public Stream<Edge<CallSite, Method>> edgesOutOf(CallSite callSite) {
        return callSiteToEdges.get(callSite).stream();
    }

    @Override
    public Stream<Edge<CallSite, Method>> edgesInTo(Method method) {
        return calleeToEdges.get(method).stream();
    }

    @Override
    public Stream<Edge<CallSite, Method>> edges() {
        return callSiteToEdges.values().stream();
    }

    @Override
    public int getNumberOfEdges() {
        return callSiteToEdges.size();
    }

    @Override
    public Stream<Method> entryMethods() {
        return entryMethods.stream();
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
        return getSuccsOf(source).contains(target);
    }

    @Override
    public Set<Method> getPredsOf(Method node) {
        return getCallersOf(node)
                .stream()
                .map(this::getContainerOf)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<Method> getSuccsOf(Method node) {
        return callSitesIn(node)
                .flatMap(cs -> getCalleesOf(cs).stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<Method> getNodes() {
        return Collections.unmodifiableSet(reachableMethods);
    }
}
