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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An object which manages the data-flow facts associated with nodes.
 *
 * @param <Node> type of nodes
 * @param <Fact> type of data-flow facts
 */
public class DataflowResult<Node, Fact> implements NodeResult<Node, Fact> {

    private final Map<Node, Fact> inFacts = new LinkedHashMap<>();

    private final Map<Node, Fact> outFacts = new LinkedHashMap<>();

    /**
     * @return the flowing-in fact of given node.
     */
    @Override
    public Fact getInFact(Node node) {
        return inFacts.get(node);
    }

    /**
     * Associates a data-flow fact with a node as its flowing-in fact.
     */
    public void setInFact(Node node, Fact fact) {
        inFacts.put(node, fact);
    }

    /**
     * @return the flowing-out fact of given node.
     */
    @Override
    public Fact getOutFact(Node node) {
        return outFacts.get(node);
    }

    /**
     * Associates a data-flow fact with a node as its flowing-out fact.
     */
    public void setOutFact(Node node, Fact fact) {
        outFacts.put(node, fact);
    }
}
