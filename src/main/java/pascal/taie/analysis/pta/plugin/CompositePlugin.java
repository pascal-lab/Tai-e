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
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.stmt.Invoke;
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
    public void setSolver(Solver solver) {
        plugins.forEach(p -> p.setSolver(solver));
    }

    @Override
    public void onPreprocess() {
        plugins.forEach(Plugin::onPreprocess);
    }

    @Override
    public void onInitialize() {
        plugins.forEach(Plugin::onInitialize);
    }

    @Override
    public void onFinish() {
        plugins.forEach(Plugin::onFinish);
    }

    @Override
    public void onPostprocess() {
        plugins.forEach(Plugin::onPostprocess);
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        plugins.forEach(p -> p.onNewPointsToSet(csVar, pts));
    }

    @Override
    public void onNewCallEdge(Edge<CSCallSite, CSMethod> edge) {
        plugins.forEach(p -> p.onNewCallEdge(edge));
    }

    @Override
    public void onNewMethod(JMethod method) {
        plugins.forEach(p -> p.onNewMethod(method));
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        plugins.forEach(p -> p.onNewCSMethod(csMethod));
    }

    @Override
    public void onUnresolvedCall(CSObj recv, Context context, Invoke invoke) {
        plugins.forEach(p -> p.onUnresolvedCall(recv, context, invoke));
    }
}
