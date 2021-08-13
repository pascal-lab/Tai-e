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
import pascal.taie.util.MutableBoolean;

import java.util.function.Predicate;

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
        MutableBoolean changed = new MutableBoolean(false);
        do {
            changed.set(false);
            cfg.nodes()
                    .filter(Predicate.not(cfg::isEntry))
                    .forEach(node -> {
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
                            cfg.outEdgesOf(node).forEach(outEdge -> {
                                if (analysis.hasEdgeTransfer()) {
                                    // apply edge transfer if necessary
                                    Fact edgeFact = result.getEdgeFact(outEdge);
                                    analysis.transferEdge(outEdge, out, edgeFact);
                                }
                                changed.set(true);
                            });
                        }
                    });
        } while (changed.get());
    }

    /**
     * No edge transfer.
     */
    private void doSolveBackward(CFG<Node> cfg, DataflowResult<Node, Fact> result) {
        MutableBoolean changed = new MutableBoolean(false);
        do {
            changed.set(false);
            cfg.nodes()
                    .filter(Predicate.not(cfg::isExit))
                    .forEach(node -> {
                        // meet incoming facts from succ to node
                        cfg.succsOf(node).forEach(succ -> {
                            Fact succIn = result.getInFact(succ);
                            Fact out = result.getOutFact(node);
                            if (out == null) {
                                out = analysis.copyFact(succIn);
                                result.setOutFact(node, out);
                            }
                            analysis.mergeInto(succIn, out);
                        });
                        // apply node transfer function
                        Fact in = result.getInFact(node);
                        Fact out = result.getOutFact(node);
                        changed.or(analysis.transferNode(node, in, out));
                    });
        } while (changed.get());
    }
}
