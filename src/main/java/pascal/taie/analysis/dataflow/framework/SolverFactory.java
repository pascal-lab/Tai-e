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

package pascal.taie.analysis.dataflow.framework;

import soot.toolkits.graph.DirectedGraph;

public enum SolverFactory {

    INSTANCE;

    public static SolverFactory v() {
        return INSTANCE;
    }

    public <Domain, Node>
    Solver<Domain, Node> newSolver(DataFlowAnalysis<Domain, Node> problem,
                                   DirectedGraph<Node> cfg) {
        return new IterativeSolver<>(problem, cfg);
    }
}
