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

import java.util.LinkedList;
import java.util.Queue;

class WorkListSolver<Node, Fact> extends Solver<Node, Fact> {

    WorkListSolver(DataflowAnalysis<Node, Fact> analysis) {
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
        Queue<Node> workList = new LinkedList<>();
        cfg.succsOf(cfg.getEntry()).forEach(workList::add);
        while (!workList.isEmpty()) {
            Node node = workList.poll();
            // meet incoming facts
            cfg.inEdgesOf(node).forEach(inEdge -> {
                Fact predOut = analysis.hasEdgeTransfer() ?
                        result.getEdgeFact(inEdge) :
                        result.getOutFact(inEdge.getSource());
                Fact in = result.getInFact(node);
                if (in == null) {
                    in = analysis.copyFact(predOut);
                    result.setInFact(node, in);
                }
                analysis.mergeInto(predOut, in);
            });
            // apply node transfer function
            Fact in = result.getInFact(node);
            Fact out = result.getOutFact(node);
            boolean changed = analysis.transferNode(node, in, out);
            if (changed) {
                cfg.outEdgesOf(node).forEach(outEdge -> {
                    if (analysis.hasEdgeTransfer()) {
                        // apply edge transfer if necessary
                        Fact edgeFact = result.getEdgeFact(outEdge);
                        analysis.transferEdge(outEdge, out, edgeFact);
                    }
                    // prepare to process successors
                    workList.add(outEdge.getTarget());
                });
            }
        }
    }

    private void doSolveBackward(CFG<Node> cfg, DataflowResult<Node, Fact> result) {
        Queue<Node> workList = new LinkedList<>();
        cfg.predsOf(cfg.getExit()).forEach(workList::add);
        while (!workList.isEmpty()) {
            Node node = workList.poll();
            // meet incoming facts
            cfg.outEdgesOf(node).forEach(outEdge -> {
                Fact succIn = analysis.hasEdgeTransfer() ?
                        result.getEdgeFact(outEdge) :
                        result.getInFact(outEdge.getTarget());
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
            boolean changed = analysis.transferNode(node, in, out);
            if (changed) {
                cfg.inEdgesOf(node).forEach(inEdge -> {
                    if (analysis.hasEdgeTransfer()) {
                        // apply edge transfer if necessary
                        Fact edgeFact = result.getEdgeFact(inEdge);
                        analysis.transferEdge(inEdge, in, edgeFact);
                    }
                    // prepare to process successors
                    workList.add(inEdge.getSource());
                });
            }
        }
    }
}
