/*
 * Panda - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Panda is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package panda.callgraph;

import java.util.Collection;
import java.util.stream.Stream;

public interface CallGraph<CallSite, Method>
        extends Iterable<Edge<CallSite, Method>> {

    /**
     * Returns the set of methods that are called by the given call site.
     */
    Collection<Method> getCallees(CallSite callSite);

    /**
     * Returns the set of call sites that can call the given method.
     */
    Collection<CallSite> getCallers(Method callee);

    /**
     * Returns the method that contains the given call site.
     */
    Method getContainerMethodOf(CallSite callSite);

    /**
     * Returns the set of call sites within the given method.
     */
    Collection<CallSite> getCallSitesIn(Method method);

    /**
     * Returns the call edges out of the given call site.
     */
    Collection<Edge<CallSite, Method>> getEdgesOf(CallSite callSite);

    /**
     * Returns all call edges in this call graph.
     */
    Stream<Edge<CallSite, Method>> getAllEdges();

    /**
     * Returns the entry methods of this call graph.
     */
    Collection<Method> getEntryMethods();

    /**
     * Returns all reachable methods in this call graph.
     */
    Collection<Method> getReachableMethods();

    /**
     * Returns if this call graph contains the given method.
     */
    boolean contains(Method method);
}
