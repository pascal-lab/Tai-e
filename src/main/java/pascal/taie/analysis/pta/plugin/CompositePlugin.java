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

package pascal.taie.analysis.pta.plugin;

import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.PointerAnalysis;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.language.classes.JMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Composite plugin which allows multiple independent plugins
 * to be used together.
 */
public class CompositePlugin implements Plugin {

    private final List<Plugin> plugins = new ArrayList<>();

    public void addPlugin(Plugin... plugins) {
        Collections.addAll(this.plugins, plugins);
    }

    @Override
    public void setPointerAnalysis(PointerAnalysis pta) {
        plugins.forEach(p -> p.setPointerAnalysis(pta));
    }

    @Override
    public void preprocess() {
        plugins.forEach(Plugin::preprocess);
    }

    @Override
    public void initialize() {
        plugins.forEach(Plugin::initialize);
    }

    @Override
    public void finish() {
        plugins.forEach(Plugin::finish);
    }

    @Override
    public void postprocess() {
        plugins.forEach(Plugin::postprocess);
    }

    @Override
    public void handleNewPointsToSet(CSVar csVar, PointsToSet pts) {
        plugins.forEach(p -> p.handleNewPointsToSet(csVar, pts));
    }

    @Override
    public void handleNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        plugins.forEach(p -> p.handleNewCallEdge(edge));
    }

    @Override
    public void handleNewMethod(JMethod method) {
        plugins.forEach(p -> p.handleNewMethod(method));
    }

    @Override
    public void handleNewCSMethod(CSMethod csMethod) {
        plugins.forEach(p -> p.handleNewCSMethod(csMethod));
    }
}
