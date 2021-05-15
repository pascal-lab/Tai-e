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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.dfa.analysis.DataflowAnalysis;
import pascal.taie.analysis.dfa.fact.DataflowResult;
import pascal.taie.analysis.graph.cfg.CFG;

import java.util.TreeSet;
import java.util.function.Predicate;

class WorkListSolver<Node, Fact> extends Solver<Node, Fact> {

    private static final Logger logger = LogManager.getLogger(WorkListSolver.class);

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
        TreeSet<Node> workList = new TreeSet<>(
                new Orderer<>(cfg, analysis.isForward()));
        cfg.nodes().filter(Predicate.not(cfg::isEntry)).forEach(workList::add);
        while (!workList.isEmpty()) {
            Node node = workList.pollFirst();
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
            if (in == null) {
                // this means that node does not have any predecessors in CFG
                logger.warn("[forward analysis]: in fact of {} is null," +
                        " skip its transfer", node);
                continue;
            }
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
        TreeSet<Node> workList = new TreeSet<>(
                new Orderer<>(cfg, analysis.isForward()));
        cfg.nodes().filter(Predicate.not(cfg::isExit)).forEach(workList::add);
        while (!workList.isEmpty()) {
            Node node = workList.pollFirst();
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
            if (out == null) {
                // this means that node does not have any successors in CFG
                logger.warn("[backward analysis]: out fact of {} is null," +
                        " skip its transfer", node);
                continue;
            }
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
