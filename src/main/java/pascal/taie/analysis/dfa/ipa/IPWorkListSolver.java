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

package pascal.taie.analysis.dfa.ipa;

import pascal.taie.util.MutableBoolean;
import pascal.taie.util.collection.SetQueue;

import java.util.Queue;

class IPWorkListSolver<Method, Node, Fact> extends
        IPSolver<Method, Node, Fact> {

    IPWorkListSolver(IPDataflowAnalysis<Method, Node, Fact> analysis) {
        super(analysis);
    }

    @Override
    protected void doSolve(ICFG<Method, Node> icfg) {
        Queue<Node> workList = new SetQueue<>();
        icfg.entryMethods().forEach(entryMethod ->
                workList.add(icfg.getEntryOf(entryMethod)));
        while (!workList.isEmpty()) {
            Node node = workList.poll();
            // meet incoming facts
            Fact in = getInFact(node);
            icfg.inEdgesOf(node).forEach(inEdge -> {
                Fact edgeFact = result.getEdgeFact(inEdge);
                if (edgeFact != null) {
                    analysis.mergeInto(edgeFact, in);
                }
            });
            MutableBoolean changed = new MutableBoolean(false);
            // apply node transfer
            Fact out = getOutFact(node, changed);
            if (icfg.isCallSite(node)) {
                changed.or(analysis.transferCall(node, in, out));
            } else {
                changed.or(analysis.transferNonCall(node, in, out));
            }
            if (changed.get()) {
                icfg.outEdgesOf(node).forEach(edge -> {
                    // apply edge transfer
                    analysis.transferEdge(edge, in, out, getEdgeFact(edge));
                    workList.add(edge.getTarget());
                });
            }
        }
    }

    private Fact getInFact(Node node) {
        Fact in = result.getInFact(node);
        if (in == null) {
            in = analysis.newInitialFact();
            result.setInFact(node, in);
        }
        return in;
    }

    private Fact getOutFact(Node node, MutableBoolean changed) {
        Fact out = result.getOutFact(node);
        if (out == null) {
            out = analysis.newInitialFact();
            result.setOutFact(node, out);
            changed.set(true);
        }
        return out;
    }

    private Fact getEdgeFact(ICFGEdge<Node> edge) {
        Fact edgeFact = result.getEdgeFact(edge);
        if (edgeFact == null) {
            edgeFact = analysis.newInitialFact();
            result.setEdgeFact(edge, edgeFact);
        }
        return edgeFact;
    }
}
