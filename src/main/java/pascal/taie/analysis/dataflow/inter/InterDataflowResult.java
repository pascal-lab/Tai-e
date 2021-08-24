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

package pascal.taie.analysis.dataflow.inter;

import pascal.taie.analysis.dataflow.fact.NodeResult;
import pascal.taie.analysis.graph.icfg.ICFGEdge;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An object which manages the data-flow facts associated with nodes and edges
 * of an ICFG.
 * TODO: merge with {@link pascal.taie.analysis.dataflow.fact.DataflowResult}
 *  by parameterize edge type?
 *
 * @param <Node> type of ICFG nodes
 * @param <Fact> type of data-flow facts
 */
public class InterDataflowResult<Node, Fact> extends NodeResult<Node, Fact> {

    private final Map<ICFGEdge<Node>, Fact> edgeFacts = new LinkedHashMap<>();

    /**
     * @return the fact of given edge.
     */
    public Fact getEdgeFact(ICFGEdge<Node> edge) {
        return edgeFacts.get(edge);
    }

    /**
     * Associates a data-flow fact with an ICFG edge.
     */
    public void setEdgeFact(ICFGEdge<Node> edge, Fact fact) {
        edgeFacts.put(edge, fact);
    }
}
