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
import pascal.taie.util.collection.Sets;

import java.util.Map;
import java.util.Set;
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

    protected final Map<CallSite, Set<Edge<CallSite, Method>>> callSiteToEdges = Maps.newMap();
    protected final Map<Method, Set<Edge<CallSite, Method>>> calleeToEdges = Maps.newMap();
    protected final Map<CallSite, Method> callSiteToContainer = Maps.newMap();
    protected final Map<Method, Set<CallSite>> callSitesIn = Maps.newMap();
    protected final Set<Method> entryMethods = Sets.newSet();
    protected final Set<Method> reachableMethods = Sets.newSet();

    @Override
    public Stream<Method> calleesOf(CallSite callSite) {
        Set<Edge<CallSite, Method>> edges = callSiteToEdges.get(callSite);
        return edges != null ?
                edges.stream().map(Edge::getCallee) : Stream.of();
    }

    @Override
    public Stream<CallSite> callersOf(Method callee) {
        Set<Edge<CallSite, Method>> edges = calleeToEdges.get(callee);
        return edges != null ?
                edges.stream().map(Edge::getCallSite) : Stream.of();
    }

    @Override
    public Method getContainerMethodOf(CallSite callSite) {
        return callSiteToContainer.get(callSite);
    }

    @Override
    public Stream<CallSite> callSitesIn(Method method) {
        return callSitesIn.getOrDefault(method, Set.of()).stream();
    }

    @Override
    public Stream<Edge<CallSite, Method>> edgesOf(CallSite callSite) {
        return callSiteToEdges.getOrDefault(callSite, Set.of()).stream();
    }

    @Override
    public Stream<Edge<CallSite, Method>> edgesTo(Method method) {
        return calleeToEdges.getOrDefault(method, Set.of()).stream();
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
        return succsOf(source).anyMatch(target::equals);
    }

    @Override
    public Stream<Method> predsOf(Method node) {
        return callersOf(node)
                .map(this::getContainerMethodOf)
                .distinct();
    }

    @Override
    public Stream<Method> succsOf(Method node) {
        return callSitesIn(node)
                .flatMap(this::calleesOf)
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
