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

package pascal.taie.analysis.dfa.solver;

import pascal.taie.analysis.dfa.analysis.DataflowAnalysis;
import pascal.taie.analysis.dfa.fact.DataflowResult;
import pascal.taie.analysis.graph.cfg.CFG;

public abstract class Solver<Node, Fact> {

    protected final DataflowAnalysis<Node, Fact> analysis;

    protected final DirectionController controller;

    protected Solver(DataflowAnalysis<Node, Fact> analysis) {
        this.analysis = analysis;
        this.controller = analysis.isForward() ?
                DirectionController.FORWARD : DirectionController.BACKWARD;
    }

    public DataflowResult<Node, Fact> solve(CFG<Node> cfg) {
        DataflowResult<Node, Fact> result = initialize(cfg);
        doSolve(cfg, result);
        return result;
    }

    protected DataflowResult<Node, Fact> initialize(CFG<Node> cfg) {
        DataflowResult<Node, Fact> result = new DataflowResult<>();
        cfg.nodes().forEach(node -> {
            result.setInFact(node, analysis.newInitialFact());
            result.setOutFact(node, analysis.newInitialFact());
            if (analysis.hasEdgeTransfer()) {
                cfg.outEdgesOf(node).forEach(edge ->
                        result.setEdgeFact(edge, analysis.newInitialFact()));
            }
        });
        return result;
    }

    protected abstract void doSolve(CFG<Node> cfg,
                                    DataflowResult<Node, Fact> result);
}
