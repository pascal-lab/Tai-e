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

public abstract class IPSolver<Method, Node, Fact> {

    protected final IPDataflowAnalysis<Method, Node, Fact> analysis;

    protected IPDataflowResult<Node, Fact> result;

    protected IPSolver(IPDataflowAnalysis<Method, Node, Fact> analysis) {
        this.analysis = analysis;
    }

    public static <Method, Node, Fact>
    IPSolver<Method, Node, Fact> makeSolver(IPDataflowAnalysis<Method, Node, Fact> analysis) {
        return new IPWorkListSolver<>(analysis);
    }

    public IPDataflowResult<Node, Fact> solve(ICFG<Method, Node> icfg) {
        initialize(icfg);
        doSolve(icfg);
        return result;
    }

    protected void initialize(ICFG<Method, Node> icfg) {
        result = new IPDataflowResult<>();
        icfg.entryMethods().forEach(entryMethod -> {
            Node entry = icfg.getEntryOf(entryMethod);
            Fact entryIn = analysis.getEntryInitialFact(icfg);
            result.setInFact(entry, entryIn);
        });
    }

    protected abstract void doSolve(ICFG<Method, Node> icfg);
}
