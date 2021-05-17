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

package pascal.taie.analysis.dfa.fact;

import java.util.LinkedHashMap;
import java.util.Map;

public class NodeResult<Node, Fact> {

    private final Map<Node, Fact> inFacts = new LinkedHashMap<>();

    private final Map<Node, Fact> outFacts = new LinkedHashMap<>();

    /**
     * @return the flowing-in fact of given node.
     */
    public Fact getInFact(Node node) {
        return inFacts.get(node);
    }

    public void setInFact(Node node, Fact fact) {
        inFacts.put(node, fact);
    }

    /**
     * @return the flowing-out fact of given node.
     */
    public Fact getOutFact(Node node) {
        return outFacts.get(node);
    }

    public void setOutFact(Node node, Fact fact) {
        outFacts.put(node, fact);
    }
}
