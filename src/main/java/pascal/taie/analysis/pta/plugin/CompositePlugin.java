/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
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
    public void onStart() {
        plugins.forEach(Plugin::onStart);
    }

    @Override
    public void onFinish() {
        plugins.forEach(Plugin::onFinish);
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
