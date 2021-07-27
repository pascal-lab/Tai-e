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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.config.ConfigException;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class TaintAnalysis implements Plugin {

    private final TaintManager factory = new TaintManager();

    private Solver solver;

    private ClassHierarchy hierarchy;

    private Context defaultCtx;

    private Set<JMethod> sources;

    private Set<JMethod> sinks;

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        this.hierarchy = solver.getHierarchy();
        this.defaultCtx = solver.getContextSelector().getDefaultContext();
        readSourcesSinks(solver.getOptions().getString("taint.sources-sinks"));
    }

    private void readSourcesSinks(String path) {
        Objects.requireNonNull(path, "Option taint.sources-sinks is not given");
        File file = new File(path);
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JavaType type = mapper.getTypeFactory()
                .constructMapType(Map.class, String.class, List.class);
        try {
            Map<String, List<String>> sourcesSinks = mapper.readValue(file, type);
            sources = sourcesSinks.get("sources")
                    .stream()
                    .map(hierarchy::getMethod)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toUnmodifiableSet());
            sinks = sourcesSinks.get("sinks")
                    .stream()
                    .map(hierarchy::getMethod)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException | NullPointerException e) {
            throw new ConfigException(
                    "Failed to read sources and sinks from " + file, e);
        }
    }

    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        Invoke callSite = edge.getCallSite().getCallSite();
        JMethod target = edge.getCallee().getMethod();
        // generate taint value from source call
        Var lhs = callSite.getLValue();
        if (lhs != null && sources.contains(target)) {
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
        for (JMethod sink : sinks) {
            result.getCallGraph().callersOf(sink).forEach(sinkCall ->
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
