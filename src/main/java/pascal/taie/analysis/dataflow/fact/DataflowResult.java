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

package pascal.taie.analysis.dataflow.fact;

import pascal.taie.analysis.graph.cfg.Edge;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Object for managing the data-flow facts associated with nodes and edges of a CFG.
 * @param <Node> type of CFG nodes
 * @param <Fact> type of data-flow facts
 */
public class DataflowResult<Node, Fact> extends NodeResult<Node, Fact> {

    private final Map<Edge<Node>, Fact> edgeFacts = new LinkedHashMap<>();

    /**
     * @return the fact of given edge.
     */
    public Fact getEdgeFact(Edge<Node> edge) {
        return edgeFacts.get(edge);
    }

    public void setEdgeFact(Edge<Node> edge, Fact fact) {
        edgeFacts.put(edge, fact);
    }
}
