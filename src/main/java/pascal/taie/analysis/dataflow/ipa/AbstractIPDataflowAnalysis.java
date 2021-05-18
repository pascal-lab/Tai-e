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

import pascal.taie.World;
import pascal.taie.analysis.InterproceduralAnalysis;
import pascal.taie.analysis.dataflow.fact.IPDataflowResult;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.CallGraphBuilder;
import pascal.taie.analysis.graph.icfg.ICFG;
import pascal.taie.analysis.graph.icfg.ICFGBuilder;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.language.classes.JMethod;

public abstract class AbstractIPDataflowAnalysis<Method, Node, Fact>
        extends InterproceduralAnalysis
        implements IPDataflowAnalysis<Method, Node, Fact> {

    protected ICFG<Method, Node> icfg;

    public AbstractIPDataflowAnalysis(AnalysisConfig config) {
        super(config);
    }

    @Override
    public Object analyze() {
        icfg = World.getResult(ICFGBuilder.ID);
        IPDataflowResult<Node, Fact> result = IPSolver.makeSolver(this).solve(icfg);
        // Temporarily print results for debugging data-flow analyses
        // TODO: replace this by proper inspection approach
        CallGraph<?, JMethod> cg = World.getResult(CallGraphBuilder.ID);
        cg.reachableMethods().forEach(m -> {
            System.out.printf("-------------------- %s (%s) --------------------%n",
                    m, getId());
            m.getIR().getStmts().forEach(stmt -> System.out.printf("L%-3d[%d:%s]: %s%n",
                    stmt.getLineNumber(), stmt.getIndex(), stmt,
                    result.getOutFact((Node) stmt)));
            System.out.println();
        });
        return result;
    }
}
