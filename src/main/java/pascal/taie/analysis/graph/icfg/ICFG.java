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

import pascal.taie.util.graph.Graph;

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
    Stream<ICFGEdge<Node>> inEdgesOf(Node node);

    /**
     * @return the outgoing edges of the given node.
     */
    @Override
    Stream<ICFGEdge<Node>> outEdgesOf(Node node);

    /**
     * @return the methods that are called by the given call site.
     */
    Stream<Method> calleesOf(Node callSite);

    /**
     * @return the return sites of the given call site.
     */
    Stream<Node> returnSitesOf(Node callSite);

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
    Stream<Node> callersOf(Method method);

    /**
     * @return the method that contains the given node.
     */
    Method getContainingMethodOf(Node node);

    /**
     * @return true if the given node is a call site, otherwise false.
     */
    boolean isCallSite(Node node);
}
