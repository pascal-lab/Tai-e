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

package pascal.taie.analysis.dataflow.solver;

import pascal.taie.analysis.dataflow.analysis.DataflowAnalysis;
import pascal.taie.analysis.dataflow.fact.DataflowResult;
import pascal.taie.analysis.graph.cfg.CFG;

/**
 * Work-list solver with optimization.
 */
class FastSolver<Node, Fact> implements Solver<Node, Fact> {

    private final DataflowAnalysis<Node, Fact> analysis;

    FastSolver(DataflowAnalysis<Node, Fact> analysis) {
        this.analysis = analysis;
    }

    @Override
    public DataflowResult<Node, Fact> solve(CFG<Node> cfg) {
        throw new UnsupportedOperationException();
    }
}
