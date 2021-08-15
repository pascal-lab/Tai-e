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
    protected void doSolve(CFG<Node> cfg, DataflowResult<Node, Fact> result) {
        if (analysis.isForward()) {
            doSolveForward(cfg, result);
        } else {
            doSolveBackward(cfg, result);
        }
    }

    private void doSolveForward(CFG<Node> cfg, DataflowResult<Node, Fact> result) {
        boolean changed;
        do {
            changed = false;
            for (Node node : cfg) {
                if (!cfg.isEntry(node)) {
                    // meet incoming facts from preds to node
                    Fact in = result.getInFact(node);
                    cfg.inEdgesOf(node).forEach(inEdge -> {
                        Fact predOut = analysis.hasEdgeTransfer() ?
                                result.getEdgeFact(inEdge) :
                                result.getOutFact(inEdge.getSource());
                        analysis.mergeInto(predOut, in);
                    });
                    // apply node transfer function
                    Fact out = result.getOutFact(node);
                    boolean c = analysis.transferNode(node, in, out);
                    if (c) {
                        changed = true;
                        cfg.outEdgesOf(node).forEach(outEdge -> {
                            if (analysis.hasEdgeTransfer()) {
                                // apply edge transfer if necessary
                                Fact edgeFact = result.getEdgeFact(outEdge);
                                analysis.transferEdge(outEdge, out, edgeFact);
                            }
                        });
                    }
                }
            }
        } while (changed);
    }

    /**
     * No edge transfer.
     */
    private void doSolveBackward(CFG<Node> cfg, DataflowResult<Node, Fact> result) {
        boolean changed;
        do {
            changed = false;
            for (Node node : cfg) {
                if (!cfg.isExit(node)) {
                    Fact out = result.getOutFact(node);
                    // meet incoming facts from succ to node
                    cfg.succsOf(node).forEach(succ -> {
                        Fact succIn = result.getInFact(succ);
                        analysis.mergeInto(succIn, out);
                    });
                    // apply node transfer function
                    Fact in = result.getInFact(node);
                    changed |= analysis.transferNode(node, in, out);
                }
            }
        } while (changed);
    }
}
