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

package pascal.taie.analysis.dataflow.analysis;

import pascal.taie.analysis.IntraproceduralAnalysis;
import pascal.taie.analysis.dataflow.fact.DataflowResult;
import pascal.taie.analysis.dataflow.solver.Solver;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.analysis.graph.cfg.CFGBuilder;
import pascal.taie.analysis.graph.cfg.Edge;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;

public abstract class AbstractDataflowAnalysis<Node, Fact>
        extends IntraproceduralAnalysis
        implements DataflowAnalysis<Node, Fact> {

    private final Solver<Node, Fact> solver;

    protected AbstractDataflowAnalysis(AnalysisConfig config) {
        super(config);
        solver = Solver.makeSolver(this);
    }

    @Override
    public DataflowResult<Node, Fact> analyze(IR ir) {
        CFG<Node> cfg = ir.getResult(CFGBuilder.ID);
        return solver.solve(cfg);
    }

    /**
     * By default, a data-flow analysis does not have edge transfer.
     */
    @Override
    public boolean hasEdgeTransfer() {
        return false;
    }

    /**
     * By default, if a data-flow analysis has edge transfer, then it needs
     * to apply transfer for every edge.
     */
    @Override
    public boolean needTransfer(Edge<Node> edge) {
        return true;
    }

    @Override
    public void transferEdge(Edge<Node> edge, Fact nodeFact, Fact edgeFact) {
    }
}
