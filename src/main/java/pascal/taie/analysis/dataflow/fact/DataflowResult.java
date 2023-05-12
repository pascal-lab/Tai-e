/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.dataflow.fact;

import pascal.taie.util.collection.Maps;

import java.util.Map;

/**
 * An object which manages the data-flow facts associated with nodes.
 *
 * @param <Node> type of nodes
 * @param <Fact> type of data-flow facts
 */
public class DataflowResult<Node, Fact> implements NodeResult<Node, Fact> {

    private final Map<Node, Fact> inFacts;

    private final Map<Node, Fact> outFacts;

    public DataflowResult(Map<Node, Fact> inFacts, Map<Node, Fact> outFacts) {
        this.inFacts = inFacts;
        this.outFacts = outFacts;
    }

    public DataflowResult() {
        this(Maps.newLinkedHashMap(), Maps.newLinkedHashMap());
    }

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
