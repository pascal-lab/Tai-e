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

package pascal.taie.analysis.graph.cfg;

import pascal.taie.ir.IR;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.graph.Graph;

import java.util.stream.Stream;

/**
 * Representation of a control-flow graph of a method.
 * @param <N> type of nodes.
 */
public interface CFG<N> extends Graph<N> {

    IR getIR();

    JMethod getMethod();

    N getEntry();

    N getExit();

//    Stream<N> normalPredsOf(N node);
//
//    Stream<N> normalSuccsOf(N node);
//
//    Stream<N> exceptionalPredsOf(N node);
//
//    Stream<N> exceptionalSuccsOf(N node);

    Stream<Edge<N>> inEdgesOf(N node);

    Stream<Edge<N>> outEdgesOf(N node);

    Stream<N> predsOf(N node, Edge.Kind kind);

    Stream<N> succsOf(N node, Edge.Kind kind);

    Stream<Edge<N>> inEdgesOf(N node, Edge.Kind kind);

    Stream<Edge<N>> outEdgesOf(N node, Edge.Kind kind);
}
