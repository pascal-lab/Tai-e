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

import pascal.taie.util.graph.Graph;

import java.util.stream.Stream;

/**
 * Representation of call graph.
 *
 * @param <CallSite> type of call sites
 * @param <Method>   type of methods
 */
public interface CallGraph<CallSite, Method> extends Graph<Method> {

    /**
     * @return the methods that are called by the given call site.
     */
    Stream<Method> calleesOf(CallSite callSite);

    /**
     * @return the call sites that invoke the given method.
     */
    Stream<CallSite> callersOf(Method callee);

    /**
     * @return the method that contains the given call site.
     */
    Method getContainerMethodOf(CallSite callSite);

    /**
     * @return the call sites within the given method.
     */
    Stream<CallSite> callSitesIn(Method method);

    /**
     * @return the call edges out of the given call site.
     */
    Stream<Edge<CallSite, Method>> edgesOf(CallSite callSite);

    /**
     * @return the call edges targeting to the given method.
     */
    Stream<Edge<CallSite, Method>> edgesTo(Method method);

    /**
     * @return all call edges in this call graph.
     */
    Stream<Edge<CallSite, Method>> edges();

    /**
     * @return the number of call graph edges in this call graph.
     */
    int getNumberOfEdges();

    /**
     * @return the entry methods of this call graph.
     */
    Stream<Method> entryMethods();

    /**
     * @return all reachable methods in this call graph.
     */
    Stream<Method> reachableMethods();

    /**
     * @return the number of reachable methods in this call graph.
     */
    int getNumberOfMethods();

    /**
     * @return true if this call graph contains the given method, otherwise false.
     */
    boolean contains(Method method);
}
