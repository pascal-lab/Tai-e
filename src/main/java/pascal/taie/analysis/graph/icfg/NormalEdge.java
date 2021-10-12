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

import pascal.taie.analysis.graph.cfg.Edge;

/**
 * The edge connecting nodes in the same method.
 * Note that This kind of edges does not include the edges from call sites
 * to their return sites, which are represented by {@link CallToReturnEdge}.
 *
 * @param <Node> type of nodes
 */
public class NormalEdge<Node> extends ICFGEdge<Node> {

    /**
     * The corresponding CFG edge, which brings the information of edge type.
     */
    private final Edge<Node> cfgEdge;

    NormalEdge(Edge<Node> edge) {
        super(edge.getSource(), edge.getTarget());
        this.cfgEdge = edge;
    }

    public Edge<Node> getCFGEdge() {
        return cfgEdge;
    }
}
