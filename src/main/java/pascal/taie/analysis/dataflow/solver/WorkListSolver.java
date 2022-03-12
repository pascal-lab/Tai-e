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

import java.util.TreeSet;

class WorkListSolver<Node, Fact> extends AbstractSolver<Node, Fact> {

    @Override
    protected void doSolveForward(DataflowAnalysis<Node, Fact> analysis,
                                  DataflowResult<Node, Fact> result) {
        CFG<Node> cfg = analysis.getCFG();
        TreeSet<Node> workList = new TreeSet<>(
                new Orderer<>(cfg, analysis.isForward()));
        cfg.forEach(node -> {
            if (!cfg.isEntry(node)) {
                workList.add(node);
            }
        });
        while (!workList.isEmpty()) {
            Node node = workList.pollFirst();
            // meet incoming facts
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
            boolean changed = analysis.transferNode(node, in, out);
            if (changed) {
                workList.addAll(cfg.getSuccsOf(node));
            }
        }
    }

    @Override
    protected void doSolveBackward(DataflowAnalysis<Node, Fact> analysis,
                                   DataflowResult<Node, Fact> result) {
        CFG<Node> cfg = analysis.getCFG();
        TreeSet<Node> workList = new TreeSet<>(
                new Orderer<>(cfg, analysis.isForward()));
        cfg.forEach(node -> {
            if (!cfg.isExit(node)) {
                workList.add(node);
            }
        });
        while (!workList.isEmpty()) {
            Node node = workList.pollFirst();
            // meet incoming facts
            Fact out = result.getOutFact(node);
            cfg.getOutEdgesOf(node).forEach(outEdge -> {
                Fact fact = result.getInFact(outEdge.getTarget());
                if (analysis.needTransferEdge(outEdge)) {
                    fact = analysis.transferEdge(outEdge, fact);
                }
                analysis.meetInto(fact, out);
            });
            // apply node transfer function
            Fact in = result.getInFact(node);
            boolean changed = analysis.transferNode(node, in, out);
            if (changed) {
                workList.addAll(cfg.getPredsOf(node));
            }
        }
    }
}
