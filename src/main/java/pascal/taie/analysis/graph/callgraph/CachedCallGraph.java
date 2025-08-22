/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.graph.callgraph;

import pascal.taie.ir.stmt.Stmt;
import pascal.taie.util.collection.Maps;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A cached view of a given callgraph.
 * Query operations on the returned callgraph "read through" to the underlying callgraph,
 * and the results are cached.
 *
 * @implNote This class is not thread-safe
 */
public class CachedCallGraph<CallSite, Method> implements CallGraph<CallSite, Method> {

    private final CallGraph<CallSite, Method> delegate;

    public CachedCallGraph(CallGraph<CallSite, Method> delegate) {
        this.delegate = delegate;
    }

    private final Map<Method, Set<Method>> predecessorCache = Maps.newMap();

    private final Map<Method, Set<Method>> successorCache = Maps.newMap();

    private final Map<Method, Set<CallSite>> callerCache = Maps.newMap();

    private final Map<CallSite, Set<Method>> calleeCache = Maps.newMap();

    private final Map<Method, Set<CallSite>> callSitesInCache = Maps.newMap();

    private final Map<CallSite, Set<Edge<CallSite, Method>>> edgesOutOfCache = Maps.newMap();

    private final Map<Method, Set<Edge<CallSite, Method>>> edgesInToCache = Maps.newMap();

    private Set<Edge<CallSite, Method>> edgesCache = null;

    @Override
    public Set<Method> getPredsOf(Method node) {
        return predecessorCache.computeIfAbsent(node, delegate::getPredsOf);
    }

    @Override
    public Set<Method> getSuccsOf(Method node) {
        return successorCache.computeIfAbsent(node, delegate::getSuccsOf);
    }

    @Override
    public Set<Method> getNodes() {
        return delegate.getNodes();
    }

    @Override
    public boolean isRelevant(Stmt stmt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Method> getResult(Stmt stmt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<CallSite> getCallersOf(Method callee) {
        return callerCache.computeIfAbsent(callee, delegate::getCallersOf);
    }

    @Override
    public Set<Method> getCalleesOf(CallSite callSite) {
        return calleeCache.computeIfAbsent(callSite, delegate::getCalleesOf);
    }

    @Override
    public Set<Method> getCalleesOfM(Method caller) {
        return getSuccsOf(caller);
    }

    @Override
    public Method getContainerOf(CallSite callSite) {
        return delegate.getContainerOf(callSite);
    }

    @Override
    public Set<CallSite> getCallSitesIn(Method method) {
        return callSitesInCache.computeIfAbsent(method, delegate::getCallSitesIn);
    }

    @Override
    public Stream<Edge<CallSite, Method>> edgesOutOf(CallSite callSite) {
        return edgesOutOfCache.computeIfAbsent(callSite,
                        cs -> delegate.edgesOutOf(cs).collect(Collectors.toUnmodifiableSet()))
                .stream();
    }

    @Override
    public Stream<Edge<CallSite, Method>> edgesInTo(Method method) {
        return edgesInToCache.computeIfAbsent(method,
                        m -> delegate.edgesInTo(m).collect(Collectors.toUnmodifiableSet()))
                .stream();
    }

    @Override
    public Stream<Edge<CallSite, Method>> edges() {
        if (edgesCache == null) {
            edgesCache = delegate.edges().collect(Collectors.toUnmodifiableSet());
        }
        return edgesCache.stream();
    }

    @Override
    public int getNumberOfEdges() {
        return delegate.getNumberOfEdges();
    }

    @Override
    public Stream<Method> entryMethods() {
        return delegate.entryMethods();
    }

    @Override
    public Stream<Method> reachableMethods() {
        return delegate.reachableMethods();
    }

    @Override
    public int getNumberOfMethods() {
        return delegate.getNumberOfMethods();
    }

    @Override
    public boolean contains(Method method) {
        return delegate.contains(method);
    }
}
