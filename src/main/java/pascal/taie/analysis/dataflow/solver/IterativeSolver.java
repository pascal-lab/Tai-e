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

class IterativeSolver<Node, Fact> extends Solver<Node, Fact> {

    public IterativeSolver(DataflowAnalysis<Node, Fact> analysis) {
        super(analysis);
    }

    @Override
    protected void doSolveForward(CFG<Node> cfg, DataflowResult<Node, Fact> result) {
        boolean changed;
        do {
            changed = false;
            for (Node node : cfg) {
                if (!cfg.isEntry(node)) {
                    // meet incoming facts from preds to node
                    Fact in = result.getInFact(node);
                    cfg.getInEdgesOf(node).forEach(inEdge -> {
                        Fact fact = result.getOutFact(inEdge.getSource());
                        if (analysis.needTransferEdge(inEdge)) {
                            fact = analysis.transferEdge(inEdge, fact);
                        }
                        analysis.meetInto(fact, in);
                    });
                    // apply node transfer function
                    Fact out = result.getOutFact(node);
                    changed |= analysis.transferNode(node, in, out);
                }
            }
        } while (changed);
    }

    @Override
    protected void doSolveBackward(CFG<Node> cfg, DataflowResult<Node, Fact> result) {
        boolean changed;
        do {
            changed = false;
            for (Node node : cfg) {
                if (!cfg.isExit(node)) {
                    Fact out = result.getOutFact(node);
                    // meet incoming facts from succ to node
                    cfg.outEdgesOf(node).forEach(outEdge -> {
                        Fact fact = result.getInFact(outEdge.getTarget());
                        if (analysis.needTransferEdge(outEdge)) {
                            fact = analysis.transferEdge(outEdge, fact);
                        }
                        analysis.meetInto(fact, out);
                    });
                    // apply node transfer function
                    Fact in = result.getInFact(node);
                    changed |= analysis.transferNode(node, in, out);
                }
            }
        } while (changed);
    }
}
