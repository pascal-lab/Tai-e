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

import pascal.taie.analysis.StmtResult;
import pascal.taie.util.graph.Graph;

import java.util.Set;
import java.util.stream.Stream;

/**
 * Representation of call graph.
 *
 * @param <CallSite> type of call sites
 * @param <Method>   type of methods
 */
public interface CallGraph<CallSite, Method> extends Graph<Method>, StmtResult<Set<Method>> {

    /**
     * @return the call sites that invoke the given method.
     */
    Set<CallSite> getCallersOf(Method callee);

    /**
     * @return the methods that are called by the given call site.
     */
    Set<Method> getCalleesOf(CallSite callSite);

    /**
     * @return the methods that are called by all call sites in the given method.
     */
    Set<Method> getCalleesOfM(Method caller);

    /**
     * @return the method that contains the given call site.
     */
    Method getContainerOf(CallSite callSite);

    /**
     * @return the call sites within the given method.
     */
    Set<CallSite> getCallSitesIn(Method method);

    /**
     * @return the call sites within the given method.
     */
    default Stream<CallSite> callSitesIn(Method method) {
        return getCallSitesIn(method).stream();
    }

    /**
     * @return the call edges out of the given call site.
     */
    Stream<Edge<CallSite, Method>> edgesOutOf(CallSite callSite);

    /**
     * @return the call edges targeting to the given method.
     */
    Stream<Edge<CallSite, Method>> edgesInTo(Method method);

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
