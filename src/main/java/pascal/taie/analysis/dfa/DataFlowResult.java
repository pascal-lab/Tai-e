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

package pascal.taie.analysis.dfa;

public interface DataFlowResult<Node, Flow> {

    /**
     * @return the in-flow of given node.
     */
    Flow getInFlow(Node node);

    void setInFlow(Node node, Flow flow);

    /**
     * @return the out-flow of given node.
     */
    Flow getOutFlow(Node node);

    void setOutFlow(Node node, Flow flow);
}
