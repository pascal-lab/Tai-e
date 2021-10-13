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

import pascal.taie.language.classes.JMethod;

/**
 * The edge connecting a call site to method entry of the callee.
 *
 * @param <Node> type of nodes
 */
public class CallEdge<Node> extends ICFGEdge<Node> {

    /**
     * Callee of the call edge.
     */
    private final JMethod callee;

    CallEdge(Node source, Node target, JMethod callee) {
        super(source, target);
        this.callee = callee;
    }

    /**
     * @return the callee of the call edge.
     */
    public JMethod getCallee() {
        return callee;
    }
}
