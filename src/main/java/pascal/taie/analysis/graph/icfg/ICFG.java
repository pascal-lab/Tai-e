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

package pascal.taie.analysis.graph.icfg;

import pascal.taie.util.graph.Graph;

import java.util.Set;
import java.util.stream.Stream;

/**
 * Represents an inter-procedural control-flow graph.
 */
public interface ICFG<Method, Node> extends Graph<Node> {

    /**
     * @return entry methods of the ICFG.
     */
    Stream<Method> entryMethods();

    /**
     * @return the incoming edges of the given node.
     */
    @Override
    Set<ICFGEdge<Node>> getInEdgesOf(Node node);

    /**
     * @return the outgoing edges of the given node.
     */
    @Override
    Set<ICFGEdge<Node>> getOutEdgesOf(Node node);

    /**
     * @return the methods that are called by the given call site.
     */
    Set<Method> getCalleesOf(Node callSite);

    /**
     * @return the return sites of the given call site.
     */
    Set<Node> getReturnSitesOf(Node callSite);

    /**
     * @return the entry node of the given method.
     */
    Node getEntryOf(Method method);

    /**
     * @return the exit node of the given method.
     */
    Node getExitOf(Method method);

    /**
     * @return the call sites that invoke the given method.
     */
    Set<Node> getCallersOf(Method method);

    /**
     * @return the method that contains the given node.
     */
    Method getContainingMethodOf(Node node);

    /**
     * @return true if the given node is a call site, otherwise false.
     */
    boolean isCallSite(Node node);
}
