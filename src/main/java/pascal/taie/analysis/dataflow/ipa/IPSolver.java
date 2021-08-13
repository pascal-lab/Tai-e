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

import pascal.taie.analysis.dataflow.fact.IPDataflowResult;
import pascal.taie.analysis.graph.icfg.ICFG;

import java.util.Set;
import java.util.stream.Collectors;

public abstract class IPSolver<Method, Node, Fact> {

    protected final IPDataflowAnalysis<Method, Node, Fact> analysis;

    protected final ICFG<Method, Node> icfg;

    protected IPDataflowResult<Node, Fact> result;

    protected IPSolver(IPDataflowAnalysis<Method, Node, Fact> analysis,
                       ICFG<Method, Node> icfg) {
        this.analysis = analysis;
        this.icfg = icfg;
    }

    public static <Method, Node, Fact>
    IPSolver<Method, Node, Fact> makeSolver(
            IPDataflowAnalysis<Method, Node, Fact> analysis,
            ICFG<Method, Node> icfg) {
        return new IPWorkListSolver<>(analysis, icfg);
    }

    public IPDataflowResult<Node, Fact> solve() {
        initialize();
        doSolve();
        return result;
    }

    protected void initialize() {
        result = new IPDataflowResult<>();
        Set<Node> entryNodes = icfg.entryMethods()
                .map(icfg::getEntryOf)
                .collect(Collectors.toUnmodifiableSet());
        icfg.nodes().forEach(node -> {
            Fact initIn, initOut;
            if (entryNodes.contains(node)) {
                initIn =  analysis.getEntryInitialFact(node);
                initOut = analysis.getEntryInitialFact(node);
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

    protected abstract void doSolve();
}
