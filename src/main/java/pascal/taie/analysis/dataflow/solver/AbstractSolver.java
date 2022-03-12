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
import pascal.taie.analysis.graph.cfg.CFGNodeMapper;
import pascal.taie.util.collection.IndexMap;

/**
 * Provides common functionalities for {@link Solver}.
 *
 * @param <Node> type of CFG nodes
 * @param <Fact> type of data-flow facts
 */
abstract class AbstractSolver<Node, Fact> implements Solver<Node, Fact> {

    @Override
    public DataflowResult<Node, Fact> solve(DataflowAnalysis<Node, Fact> analysis) {
        DataflowResult<Node, Fact> result = initialize(analysis);
        doSolve(analysis, result);
        return result;
    }

    /**
     * Creates and initializes a new data-flow result for given CFG.
     *
     * @return the initialized data-flow result
     */
    private DataflowResult<Node, Fact> initialize(DataflowAnalysis<Node, Fact> analysis) {
        CFG<Node> cfg = analysis.getCFG();
        var mapper = new CFGNodeMapper<>(cfg);
        DataflowResult<Node, Fact> result = new DataflowResult<>(
                new IndexMap<>(mapper, cfg.getNumberOfNodes()),
                new IndexMap<>(mapper, cfg.getNumberOfNodes()));
        if (analysis.isForward()) {
            initializeForward(analysis, result);
        } else {
            initializeBackward(analysis, result);
        }
        return result;
    }

    protected void initializeForward(DataflowAnalysis<Node, Fact> analysis,
                                     DataflowResult<Node, Fact> result) {
        CFG<Node> cfg = analysis.getCFG();
        // initialize entry
        Node entry = cfg.getEntry();
        result.setInFact(entry, analysis.newBoundaryFact());
        result.setOutFact(entry, analysis.newBoundaryFact());
        cfg.forEach(node -> {
            // skip entry which has been initialized
            if (cfg.isEntry(node)) {
                return;
            }
            // initialize in & out fact
            result.setInFact(node, analysis.newInitialFact());
            result.setOutFact(node, analysis.newInitialFact());
        });
    }

    protected void initializeBackward(DataflowAnalysis<Node, Fact> analysis,
                                      DataflowResult<Node, Fact> result) {
        CFG<Node> cfg = analysis.getCFG();
        // initialize exit
        Node exit = cfg.getExit();
        result.setInFact(exit, analysis.newBoundaryFact());
        result.setOutFact(exit, analysis.newBoundaryFact());
        cfg.forEach(node -> {
            // skip exit which has been initialized
            if (cfg.isExit(node)) {
                return;
            }
            // initialize in fact
            result.setInFact(node, analysis.newInitialFact());
            result.setOutFact(node, analysis.newInitialFact());
        });
    }

    /**
     * Solves the data-flow problem for given analysis.
     */
    private void doSolve(DataflowAnalysis<Node, Fact> analysis,
                         DataflowResult<Node, Fact> result) {
        if (analysis.isForward()) {
            doSolveForward(analysis, result);
        } else {
            doSolveBackward(analysis, result);
        }
    }

    protected abstract void doSolveForward(DataflowAnalysis<Node, Fact> analysis,
                                           DataflowResult<Node, Fact> result);

    protected abstract void doSolveBackward(DataflowAnalysis<Node, Fact> analysis,
                                            DataflowResult<Node, Fact> result);
}
