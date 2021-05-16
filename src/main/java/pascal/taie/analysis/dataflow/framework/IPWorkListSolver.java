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

package pascal.taie.analysis.dataflow.framework;

import pascal.taie.analysis.graph.icfg.ICFG;
import pascal.taie.util.collection.StreamUtils;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Queue;

public class IPWorkListSolver<Domain, Method, Node>
        extends IPSolver<Domain, Method, Node> {

    public IPWorkListSolver(IPDataFlowAnalysis<Domain, Method, Node> analysis,
                            ICFG<Method, Node> icfg) {
        super(analysis, icfg);
        inFlow = new LinkedHashMap<>();
        outFlow = new LinkedHashMap<>();
        edgeFlow = new LinkedHashMap<>();
    }

    @Override
    protected void solveFixedPoint(ICFG<Method, Node> icfg) {
        // TODO - A little too much special cases, to be refactored
        Queue<Node> workList = new LinkedList<>(icfg.getHeads());
        while (!workList.isEmpty()) {
            Node node = workList.remove();
            Domain in;
            if (StreamUtils.isEmpty(icfg.inEdgesOf(node))) { // heads of entry methods
                in = inFlow.get(node);
            } else { // other nodes
                in = icfg.inEdgesOf(node)
                        .map(edgeFlow::get)
                        .reduce(analysis.newInitialFlow(), analysis::meet);
                inFlow.put(node, in);
            }
            boolean changed = false;
            Domain out;
            if (outFlow.containsKey(node)) {
                out = outFlow.get(node);
            } else {
                // node has not been visited before
                out = analysis.newInitialFlow();
                outFlow.put(node, out);
                changed = true;
            }
            if (icfg.isCallSite(node)) {
                changed |= analysis.transferCallNode(node, in, out);
            } else {
                changed |= analysis.transfer(node, in, out);
            }
            if (changed) {
                icfg.outEdgesOf(node).forEach(edge -> {
                    analysis.transferEdge(edge, in, out, edgeFlow.get(edge));
                    workList.add(edge.getTarget());
                });
            }
        }
    }
}
