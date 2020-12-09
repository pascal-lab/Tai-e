/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.icfg;

import soot.toolkits.graph.DirectedGraph;

import java.util.Collection;

/**
 * Represents an interprocedural control-flow graph.
 */
public interface ICFG<Method, Node> extends DirectedGraph<Node> {

    Collection<Edge<Node>> getInEdgesOf(Node node);

    Collection<Edge<Node>> getOutEdgesOf(Node node);

    Collection<Method> getEntryMethods();

    Collection<Method> getCalleesOf(Node callSite);

    Collection<Node> getEntriesOf(Method method);

    Collection<Node> getCallersOf(Method method);

    Collection<Node> getExitsOf(Method method);

    Collection<Node> getReturnSitesOf(Node callSite);

    Method getContainingMethodOf(Node node);

    boolean isCallSite(Node node);
}
