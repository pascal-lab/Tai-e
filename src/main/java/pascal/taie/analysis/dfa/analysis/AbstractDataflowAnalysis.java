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

package pascal.taie.analysis.dfa.analysis;

import pascal.taie.analysis.IntraproceduralAnalysis;
import pascal.taie.analysis.dfa.fact.DataflowResult;
import pascal.taie.analysis.dfa.solver.Solver;
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
        DataflowResult<Node, Fact> result = solver.solve(cfg);
        // Temporarily print results for debugging data-flow analyses
        System.out.printf("-------------------- %s (%s) --------------------%n",
                ir.getMethod(), getId());
        ir.getStmts().forEach(stmt -> System.out.printf("L%-3d[%s]: %s%n",
                stmt.getLineNumber(), stmt, result.getOutFact((Node) stmt)));
        System.out.println();
        return result;
    }

    /**
     * By default, a data-flow analysis does not have edge transfer.
     */
    @Override
    public boolean hasEdgeTransfer() {
        return false;
    }

    @Override
    public void transferEdge(Edge<Node> edge, Fact nodeFact, Fact edgeFact) {
    }
}
