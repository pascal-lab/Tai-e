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

package pascal.taie.analysis.dataflow.ipa;

import pascal.taie.analysis.graph.icfg.ICFG;
import pascal.taie.util.collection.SetQueue;

import java.util.Queue;

class IPWorkListSolver<Method, Node, Fact> extends
        IPSolver<Method, Node, Fact> {

    IPWorkListSolver(IPDataflowAnalysis<Method, Node, Fact> analysis,
                     ICFG<Method, Node> icfg) {
        super(analysis, icfg);
    }

    @Override
    protected void doSolve() {
        Queue<Node> workList = new SetQueue<>();
        icfg.nodes().forEach(workList::add);
        while (!workList.isEmpty()) {
            Node node = workList.poll();
            // meet incoming facts
            Fact in = result.getInFact(node);
            icfg.inEdgesOf(node).forEach(inEdge -> {
                Fact edgeFact = result.getEdgeFact(inEdge);
                analysis.mergeInto(edgeFact, in);
            });
            Fact out = result.getOutFact(node);
            boolean changed = analysis.transferNode(node, in, out);
            if (changed) {
                icfg.outEdgesOf(node).forEach(edge -> {
                    // apply edge transfer
                    analysis.transferEdge(edge, in, out,
                            result.getEdgeFact(edge));
                    workList.add(edge.getTarget());
                });
            }
        }
    }
}
