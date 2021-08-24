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
        DataflowResult<Node, Fact> result =
                new DataflowResult<>(analysis.hasEdgeTransfer());
        if (analysis.isForward()) {
            initializeForward(cfg, result);
        } else {
            initializeBackward(cfg, result);
        }
        return result;
    }

    protected void initializeForward(CFG<Node> cfg, DataflowResult<Node, Fact> result) {
        // initialize entry
        Node entry = cfg.getEntry();
        result.setInFact(entry, analysis.newBoundaryFact(cfg));
        result.setOutFact(entry, analysis.newBoundaryFact(cfg));
        if (analysis.hasEdgeTransfer()) {
            cfg.outEdgesOf(entry).forEach(edge ->
                    result.setEdgeFact(edge, analysis.newBoundaryFact(cfg)));
        }
        cfg.forEach(node -> {
            // skip entry which has been initialized
            if (cfg.isEntry(node)) {
                return;
            }
            // initialize in & out fact
            result.setInFact(node, analysis.newInitialFact());
            result.setOutFact(node, analysis.newInitialFact());
            // initialize edge fact
            if (analysis.hasEdgeTransfer()) {
                cfg.outEdgesOf(node).forEach(edge ->
                        result.setEdgeFact(edge, analysis.newInitialFact()));
            }
        });
    }

    protected void initializeBackward(CFG<Node> cfg, DataflowResult<Node, Fact> result) {
        // initialize exit
        Node exit = cfg.getExit();
        result.setInFact(exit, analysis.newBoundaryFact(cfg));
        result.setOutFact(exit, analysis.newBoundaryFact(cfg));
        if (analysis.hasEdgeTransfer()) {
            cfg.inEdgesOf(exit).forEach(edge ->
                    result.setEdgeFact(edge, analysis.newBoundaryFact(cfg)));
        }
        cfg.forEach(node -> {
            // skip exit which has been initialized
            if (cfg.isExit(node)) {
                return;
            }
            // initialize in fact
            result.setInFact(node, analysis.newInitialFact());
            result.setOutFact(node, analysis.newInitialFact());
            // initialize edge fact
            if (analysis.hasEdgeTransfer()) {
                cfg.inEdgesOf(node).forEach(edge ->
                        result.setEdgeFact(edge, analysis.newInitialFact()));
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
