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

import java.util.Collection;
import java.util.stream.Stream;

public interface CallGraph<CallSite, Method> extends Graph<Method> {

    /**
     * @return the set of methods that are called by the given call site.
     */
    Collection<Method> getCallees(CallSite callSite);

    /**
     * @return the set of call sites that can call the given method.
     */
    Collection<CallSite> getCallers(Method callee);

    /**
     * @return the method that contains the given call site.
     */
    Method getContainerMethodOf(CallSite callSite);

    /**
     * @return the set of call sites within the given method.
     */
    Collection<CallSite> getCallSitesIn(Method method);

    /**
     * @return the call edges out of the given call site.
     */
    Collection<Edge<CallSite, Method>> getEdgesOf(CallSite callSite);

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
    Collection<Method> getEntryMethods();

    /**
     * @return stream of all reachable methods in this call graph.
     */
    Stream<Method> reachableMethods();

    /**
     * @return the number of reachable methods in this call graph.
     */
    int getNumberOfMethods();

    /**
     * @return if this call graph contains the given method.
     */
    boolean contains(Method method);
}
