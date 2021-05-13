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
        Queue<Node> workList = new LinkedList<>();
        cfg.nodes().forEach(workList::add);
        while (!workList.isEmpty()) {
            Node node = workList.poll();
            controller.meetIncomingFacts(result, cfg, node);
            Fact in = result.getInFact(node);
            Fact out = result.getOutFact(node);
            boolean changed = analysis.transferNode(node, in, out);
            if (changed) {
                if (analysis.hasEdgeTransfer()) {
                    controller.applyEdgeTransfer(result, cfg, node);
                }
                controller.getOutgoingNodes(cfg, node)
                        .forEach(workList::add);
            }
        }
    }
}
