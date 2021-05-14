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

    protected Solver(DataflowAnalysis<Node, Fact> analysis) {
        this.analysis = analysis;
    }

    public static <Node, Fact> Solver<Node, Fact> makeSolver(
            DataflowAnalysis<Node, Fact> analysis) {
        return new WorkListSolver<>(analysis);
    }

    public DataflowResult<Node, Fact> solve(CFG<Node> cfg) {
        DataflowResult<Node, Fact> result = initialize(cfg);
        doSolve(cfg, result);
        return result;
    }

    protected DataflowResult<Node, Fact> initialize(CFG<Node> cfg) {
        DataflowResult<Node, Fact> result = new DataflowResult<>();
        if (analysis.isForward()) {
            initializeForward(result, cfg);
        } else {
            initializeBackward(result, cfg);
        }
        return result;
    }

    private void initializeForward(DataflowResult<Node, Fact> result,
                                   CFG<Node> cfg) {
        cfg.nodes().forEach(node -> {
            Fact initFact = cfg.isEntry(node) ?
                    analysis.getEntryInitialFact(cfg) : analysis.newInitialFact();
            result.setOutFact(node, initFact);
            cfg.outEdgesOf(node).forEach(edge -> {
                Fact edgeFact = analysis.newInitialFact();
                result.setEdgeFact(edge, edgeFact);
                if (cfg.isEntry(node) && analysis.hasEdgeTransfer()) {
                    // perform edge transfer for entry node
                    analysis.transferEdge(edge, initFact, edgeFact);
                }
            });
        });
    }

    private void initializeBackward(DataflowResult<Node, Fact> result,
                                   CFG<Node> cfg) {
        cfg.nodes().forEach(node -> {
            Fact initFact = cfg.isExit(node) ?
                    analysis.getEntryInitialFact(cfg) : analysis.newInitialFact();
            result.setInFact(node, initFact);
            cfg.inEdgesOf(node).forEach(edge -> {
                Fact edgeFact = analysis.newInitialFact();
                result.setEdgeFact(edge, edgeFact);
                if (cfg.isExit(node) && analysis.hasEdgeTransfer()) {
                    // perform edge transfer for exit node
                    analysis.transferEdge(edge, initFact, edgeFact);
                }
            });
        });
    }

    protected abstract void doSolve(CFG<Node> cfg,
                                    DataflowResult<Node, Fact> result);
}
