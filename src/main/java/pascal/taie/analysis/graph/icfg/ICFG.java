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

package pascal.taie.analysis.graph.icfg;

import soot.toolkits.graph.DirectedGraph;

import java.util.Collection;

/**
 * Represents an inter-procedural control-flow graph.
 * TODO: 1. return Stream instead of Collection
 *       2. return single entry & exit
 */
public interface ICFG<Method, Node> extends DirectedGraph<Node> {

    Collection<ICFGEdge<Node>> getInEdgesOf(Node node);

    Collection<ICFGEdge<Node>> getOutEdgesOf(Node node);

    Collection<Method> getEntryMethods();

    Collection<Method> getCalleesOf(Node callSite);

    Collection<Node> getEntriesOf(Method method);

    Collection<Node> getCallersOf(Method method);

    Collection<Node> getExitsOf(Method method);

    Collection<Node> getReturnSitesOf(Node callSite);

    Method getContainingMethodOf(Node node);

    boolean isCallSite(Node node);
}
