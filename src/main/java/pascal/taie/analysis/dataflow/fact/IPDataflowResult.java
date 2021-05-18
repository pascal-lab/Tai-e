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

import pascal.taie.analysis.graph.icfg.ICFGEdge;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * TODO: merge with {@link pascal.taie.analysis.dataflow.fact.DataflowResult}
 *  by parameterize edge type?
 */
public class IPDataflowResult<Node, Fact> extends NodeResult<Node, Fact> {

    private final Map<ICFGEdge<Node>, Fact> edgeFacts = new LinkedHashMap<>();

    /**
     * @return the fact of given edge.
     */
    public Fact getEdgeFact(ICFGEdge<Node> edge) {
        return edgeFacts.get(edge);
    }

    public void setEdgeFact(ICFGEdge<Node> edge, Fact fact) {
        edgeFacts.put(edge, fact);
    }
}
