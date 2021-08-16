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

    private DataflowResult<Node, Fact> initialize(CFG<Node> cfg) {
        DataflowResult<Node, Fact> result = new DataflowResult<>();
        if (analysis.isForward()) {
            initializeForward(cfg, result);
        } else {
            initializeBackward(cfg, result);
        }
        return result;
    }

    private void initializeForward(CFG<Node> cfg, DataflowResult<Node, Fact> result) {
        cfg.forEach(node -> {
            Fact initIn, initOut;
            // initialize in fact
            if (cfg.isEntry(node)) {
                initIn = analysis.newBoundaryFact(cfg);
                initOut = analysis.newBoundaryFact(cfg);
            } else {
                initIn = analysis.newInitialFact();
                initOut = analysis.newInitialFact();
            }
            // initialize in & out fact
            result.setInFact(node, initIn);
            result.setOutFact(node, initOut);
            if (analysis.hasEdgeTransfer()) {
                cfg.outEdgesOf(node).forEach(edge -> {
                    Fact edgeFact = analysis.newInitialFact();
                    // initialize edge fact
                    result.setEdgeFact(edge, edgeFact);
                    if (cfg.isEntry(node)) {
                        // entry node may not be transferred by the solver,
                        // thus we apply edge transfer for it in advance
                        analysis.transferEdge(edge, initOut, edgeFact);
                    }
                });
            }
        });
    }

    private void initializeBackward(CFG<Node> cfg, DataflowResult<Node, Fact> result) {
        cfg.forEach(node -> {
            Fact initIn, initOut;
            // initialize in fact
            if (cfg.isExit(node)) {
                initIn = analysis.newBoundaryFact(cfg);
                initOut = analysis.newBoundaryFact(cfg);
            } else {
                initIn = analysis.newInitialFact();
                initOut = analysis.newInitialFact();
            }
            // initialize in & out fact
            result.setInFact(node, initIn);
            result.setOutFact(node, initOut);
            if (analysis.hasEdgeTransfer()) {
                // apply edge transfer for exit node
                cfg.inEdgesOf(node).forEach(edge -> {
                    Fact edgeFact = analysis.newInitialFact();
                    // initialize edge fact
                    result.setEdgeFact(edge, edgeFact);
                    if (cfg.isExit(node)) {
                        // exit node may not be transferred by the solver,
                        // thus we apply edge transfer for it in advance
                        analysis.transferEdge(edge, initIn, edgeFact);
                    }
                });
            }
        });
    }

    private void doSolve(CFG<Node> cfg, DataflowResult<Node, Fact> result) {
        if (analysis.isForward()) {
            doSolveForward(cfg, result);
        } else {
            doSolveBackward(cfg, result);
        }
    }

    protected abstract void doSolveForward(CFG<Node> cfg, DataflowResult<Node,Fact> result);

    protected abstract void doSolveBackward(CFG<Node> cfg, DataflowResult<Node,Fact> result);
}
