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

class WorkListSolver<Node, Fact> extends Solver<Node, Fact> {

    WorkListSolver(DataflowAnalysis<Node, Fact> analysis) {
        super(analysis);
    }

    @Override
    protected void doSolveForward(CFG<Node> cfg, DataflowResult<Node, Fact> result) {
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
                cfg.succsOf(node).forEach(workList::add);
            }
        }
    }

    @Override
    protected void doSolveBackward(CFG<Node> cfg, DataflowResult<Node, Fact> result) {
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
            cfg.outEdgesOf(node).forEach(outEdge -> {
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
                cfg.getPredsOf(node).forEach(workList::add);
            }
        }
    }
}
