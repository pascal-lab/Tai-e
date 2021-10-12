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
 * The edge connecting a call site and its return site.
 *
 * @param <Node> type of nodes
 */
public class CallToReturnEdge<Node> extends ICFGEdge<Node> {

    public CallToReturnEdge(Edge<Node> edge) {
        super(edge.getSource(), edge.getTarget());
    }
}
