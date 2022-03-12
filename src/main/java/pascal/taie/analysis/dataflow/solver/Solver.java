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

/**
 * Interface of data-flow analysis solver.
 *
 * @param <Node> type of control-flow graph nodes
 * @param <Fact> type of data facts
 */
public interface Solver<Node, Fact> {

    /**
     * The default solver.
     */
    @SuppressWarnings("rawtypes")
    Solver SOLVER = new FastSolver<>();

    /**
     * Static factory method for obtaining a solver.
     */
    @SuppressWarnings("unchecked")
    static <Node, Fact> Solver<Node, Fact> getSolver() {
        return (Solver<Node, Fact>) SOLVER;
    }

    /**
     * Solves the given analysis problem.
     *
     * @return the data-flow analysis result
     */
    DataflowResult<Node, Fact> solve(DataflowAnalysis<Node, Fact> analysis);
}
