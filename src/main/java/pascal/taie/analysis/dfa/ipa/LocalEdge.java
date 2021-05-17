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

import pascal.taie.analysis.graph.cfg.Edge;

public class LocalEdge<Node> extends ICFGEdge<Node> {

    /**
     * The corresponding CFG edge, which brings the information of edge type.
     */
    private final Edge<Node> cfgEdge;

    LocalEdge(Edge<Node> edge) {
        super(Kind.LOCAL, edge.getSource(), edge.getTarget());
        this.cfgEdge = edge;
    }

    public Edge<Node> getCFGEdge() {
        return cfgEdge;
    }
}
