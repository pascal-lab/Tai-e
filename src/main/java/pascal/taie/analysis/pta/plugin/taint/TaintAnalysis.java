/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TaintAnalysis implements Plugin {

    private final TaintManager factory = new TaintManager();

    private TaintConfig taintConfig;

    private Solver solver;

    private Context defaultCtx;

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        this.defaultCtx = solver.getContextSelector().getDefaultContext();
        this.taintConfig = TaintConfig.readConfig(
                solver.getOptions().getString("taint.config"),
                solver.getHierarchy());
    }

    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        Invoke callSite = edge.getCallSite().getCallSite();
        JMethod target = edge.getCallee().getMethod();
        // generate taint value from source call
        Var lhs = callSite.getLValue();
        if (lhs != null && taintConfig.getSources().contains(target)) {
            Obj taint = factory.getTaint(callSite, target.getReturnType());
            solver.addVarPointsTo(edge.getCallSite().getContext(), lhs,
                    defaultCtx, taint);
        }
    }

    @Override
    public void onFinish() {
        // collect taint flows
        PointerAnalysisResult result = solver.getResult();
        List<TaintFlow> taintFlows = new ArrayList<>();
        for (MethodParam sink : taintConfig.getSinks()) {
            result.getCallGraph().callersOf(sink.getMethod()).forEach(sinkCall ->
                    sinkCall.getInvokeExp()
                            .getArgs()
                            .stream()
                            .map(result::getPointsToSet)
                            .flatMap(Set::stream)
                            .filter(TaintManager::isTaint)
                            .map(TaintManager::getSourceCall)
                            .map(sourceCall -> new TaintFlow(sourceCall, sinkCall))
                            .forEach(taintFlows::add)
            );
        }
        // report taint flows
        taintFlows.stream()
                .distinct()
                .sorted()
                .forEach(System.out::println);
    }
}
