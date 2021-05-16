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

package pascal.taie.analysis.dfa.ipa;

import pascal.taie.util.graph.Graph;

import java.util.stream.Stream;

/**
 * Represents an inter-procedural control-flow graph.
 * TODO: 1. implements Graph<Node> interface
 *       2. return single entry & exit
 */
public interface ICFG<Method, Node> extends Graph<Node> {

    Stream<ICFGEdge<Node>> inEdgesOf(Node node);

    Stream<ICFGEdge<Node>> outEdgesOf(Node node);

    Stream<Method> entryMethods();

    Stream<Method> calleesOf(Node callSite);

    Stream<Node> entriesOf(Method method);

    Stream<Node> callersOf(Method method);

    Stream<Node> exitsOf(Method method);

    Stream<Node> returnSitesOf(Node callSite);

    Method getContainingMethodOf(Node node);

    boolean isCallSite(Node node);
}
