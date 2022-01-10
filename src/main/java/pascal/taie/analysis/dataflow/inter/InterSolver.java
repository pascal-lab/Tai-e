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

package pascal.taie.analysis.dataflow.inter;

import pascal.taie.analysis.dataflow.fact.DataflowResult;
import pascal.taie.analysis.graph.icfg.ICFG;
import pascal.taie.util.collection.SetQueue;

import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Solver for inter-procedural data-flow analysis.
 * The workload of inter-procedural analysis is heavy, thus we always
 * adopt work-list algorithm for efficiency.
 */
class InterSolver<Method, Node, Fact> {

    private final InterDataflowAnalysis<Node, Fact> analysis;

    private final ICFG<Method, Node> icfg;

    private DataflowResult<Node, Fact> result;

    private Queue<Node> workList;

    InterSolver(InterDataflowAnalysis<Node, Fact> analysis,
                ICFG<Method, Node> icfg) {
        this.analysis = analysis;
        this.icfg = icfg;
    }

    DataflowResult<Node, Fact> solve() {
        result = new DataflowResult<>();
        initialize();
        doSolve();
        return result;
    }

    private void initialize() {
        Set<Node> entryNodes = icfg.entryMethods()
                .map(icfg::getEntryOf)
                .collect(Collectors.toSet());
        entryNodes.forEach(entry -> {
            result.setInFact(entry, analysis.newBoundaryFact(entry));
            result.setOutFact(entry, analysis.newBoundaryFact(entry));
        });
        icfg.forEach(node -> {
            if (entryNodes.contains(node)) {
                return;
            }
            result.setInFact(node, analysis.newInitialFact());
            result.setOutFact(node, analysis.newInitialFact());
        });
    }

    private void doSolve() {
        workList = new SetQueue<>();
        icfg.forEach(workList::add);
        while (!workList.isEmpty()) {
            Node node = workList.poll();
            // meet incoming facts
            Fact in = result.getInFact(node);
            icfg.getInEdgesOf(node).forEach(inEdge -> {
                Fact predOut = result.getOutFact(inEdge.getSource());
                analysis.meetInto(analysis.transferEdge(inEdge, predOut), in);
            });
            Fact out = result.getOutFact(node);
            boolean changed = analysis.transferNode(node, in, out);
            if (changed) {
                propagate(node);
            }
        }
    }

    void propagate(Node node) {
        icfg.getSuccsOf(node).forEach(workList::add);
    }

    Fact getOutFact(Node node) {
        return result.getOutFact(node);
    }
}
