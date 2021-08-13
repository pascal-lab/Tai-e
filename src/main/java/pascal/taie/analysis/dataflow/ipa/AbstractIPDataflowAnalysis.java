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
import pascal.taie.analysis.graph.icfg.CallEdge;
import pascal.taie.analysis.graph.icfg.ICFG;
import pascal.taie.analysis.graph.icfg.ICFGBuilder;
import pascal.taie.analysis.graph.icfg.ICFGEdge;
import pascal.taie.analysis.graph.icfg.LocalEdge;
import pascal.taie.analysis.graph.icfg.ReturnEdge;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.language.classes.JMethod;

public abstract class AbstractIPDataflowAnalysis<Method, Node, Fact>
        extends InterproceduralAnalysis
        implements IPDataflowAnalysis<Method, Node, Fact> {

    protected ICFG<Method, Node> icfg;

    protected IPSolver<Method, Node, Fact> solver;

    public AbstractIPDataflowAnalysis(AnalysisConfig config) {
        super(config);
    }

    @Override
    public boolean transferNode(Node node, Fact in, Fact out) {
        if (icfg.isCallSite(node)) {
            return transferCall(node, in, out);
        } else {
            return transferNonCall(node, in, out);
        }
    }

    protected abstract boolean transferCall(Node node, Fact in, Fact out);

    protected abstract boolean transferNonCall(Node node, Fact in, Fact out);

    @Override
    public void transferEdge(
            ICFGEdge<Node> edge, Fact in, Fact out, Fact edgeFact) {
        if (edge instanceof LocalEdge) {
            transferLocalEdge((LocalEdge<Node>) edge, out, edgeFact);
        } else if (edge instanceof CallEdge) {
            transferCallEdge((CallEdge<Node>) edge, in, edgeFact);
        } else {
            transferReturnEdge((ReturnEdge<Node>) edge, out, edgeFact);
        }
    }

    protected abstract void transferLocalEdge(LocalEdge<Node> edge, Fact out, Fact edgeFact);

    protected abstract void transferCallEdge(CallEdge<Node> edge, Fact callSiteIn, Fact edgeFact);

    protected abstract void transferReturnEdge(ReturnEdge<Node> edge, Fact returnOut, Fact edgeFact);

    @Override
    public Object analyze() {
        icfg = World.getResult(ICFGBuilder.ID);
        solver = new IPSolver<>(this, icfg);
        IPDataflowResult<Node, Fact> result = solver.solve();
        // Temporarily print results for debugging data-flow analyses
        // TODO: replace this by proper inspection approach
        CallGraph<?, JMethod> cg = World.getResult(CallGraphBuilder.ID);
        cg.reachableMethods()
                .filter(m -> m.getDeclaringClass().isApplication())
                .forEach(m -> {
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
