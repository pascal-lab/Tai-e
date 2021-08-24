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
 *
 * @param <N> type of CFG nodes.
 */
public interface CFG<N> extends Graph<N> {

    /**
     * @return the IR of the method this CFG represents.
     */
    IR getIR();

    /**
     * @return the method this CFG represents.
     */
    JMethod getMethod();

    /**
     * @return the entry node of this CFG.
     */
    N getEntry();

    /**
     * @return the exit node of this CFG.
     */
    N getExit();

    /**
     * @return true if the given node is the entry of this CFG, otherwise false.
     */
    boolean isEntry(N node);

    /**
     * @return true if the given node is the exit of this CFG, otherwise false.
     */
    boolean isExit(N node);

    /**
     * @return incoming edges of the given node.
     */
    Stream<Edge<N>> inEdgesOf(N node);

    /**
     * @return outgoing edges of the given node.
     */
    Stream<Edge<N>> outEdgesOf(N node);
}
