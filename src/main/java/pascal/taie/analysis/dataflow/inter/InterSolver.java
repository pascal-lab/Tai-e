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

    private InterDataflowResult<Node, Fact> result;

    private Queue<Node> workList;

    InterSolver(InterDataflowAnalysis<Node, Fact> analysis,
                ICFG<Method, Node> icfg) {
        this.analysis = analysis;
        this.icfg = icfg;
    }

    InterDataflowResult<Node, Fact> solve() {
        result = new InterDataflowResult<>();
        initialize();
        doSolve();
        return result;
    }

    private void initialize() {
        Set<Node> entryNodes = icfg.entryMethods()
                .map(icfg::getEntryOf)
                .collect(Collectors.toUnmodifiableSet());
        icfg.forEach(node -> {
            Fact initIn, initOut;
            if (entryNodes.contains(node)) {
                initIn = analysis.newBoundaryFact(node);
                initOut = analysis.newBoundaryFact(node);
            } else {
                initIn = analysis.newInitialFact();
                initOut = analysis.newInitialFact();
            }
            result.setInFact(node, initIn);
            result.setOutFact(node, initOut);
            icfg.outEdgesOf(node).forEach(edge -> {
                Fact edgeFact = analysis.newInitialFact();
                result.setEdgeFact(edge, edgeFact);
                if (entryNodes.contains(node)) {
                    analysis.transferEdge(edge, initIn, initOut, edgeFact);
                }
            });
        });
    }

    private void doSolve() {
        workList = new SetQueue<>();
        icfg.forEach(workList::add);
        while (!workList.isEmpty()) {
            Node node = workList.poll();
            // meet incoming facts
            Fact in = result.getInFact(node);
            icfg.inEdgesOf(node).forEach(inEdge -> {
                Fact edgeFact = result.getEdgeFact(inEdge);
                analysis.meetInto(edgeFact, in);
            });
            Fact out = result.getOutFact(node);
            boolean changed = analysis.transferNode(node, in, out);
            if (changed) {
                propagate(node);
            }
        }
    }

    void propagate(Node node) {
        Fact in = result.getInFact(node);
        Fact out = result.getOutFact(node);
        icfg.outEdgesOf(node).forEach(edge -> {
            // apply edge transfer
            analysis.transferEdge(edge, in, out, result.getEdgeFact(edge));
            workList.add(edge.getTarget());
        });
    }

    Fact getOutFact(Node node) {
        return result.getOutFact(node);
    }
}
