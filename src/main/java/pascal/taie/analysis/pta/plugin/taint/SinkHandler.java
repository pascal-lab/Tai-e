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

package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.ir.exp.Var;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Handles sinks in taint analysis.
 */
class SinkHandler {

    private final Solver solver;

    private final TaintManager manager;

    private final List<Sink> sinks;

    SinkHandler(Solver solver, TaintManager manager, List<Sink> sinks) {
        this.solver = solver;
        this.manager = manager;
        this.sinks = sinks;
    }

    Set<TaintFlow> collectTaintFlows() {
        PointerAnalysisResult result = solver.getResult();
        Set<TaintFlow> taintFlows = new TreeSet<>();
        sinks.forEach(sink -> {
            int i = sink.index();
            result.getCallGraph()
                    .edgesInTo(sink.method())
                    // TODO: handle other call edges
                    .filter(e -> e.getKind() != CallKind.OTHER)
                    .map(Edge::getCallSite)
                    .forEach(sinkCall -> {
                        Var arg = IndexUtils.getVar(sinkCall, i);
                        SinkPoint sinkPoint = new SinkPoint(sinkCall, i);
                        result.getPointsToSet(arg)
                                .stream()
                                .filter(manager::isTaint)
                                .map(manager::getSourcePoint)
                                .map(sourcePoint -> new TaintFlow(sourcePoint, sinkPoint))
                                .forEach(taintFlows::add);
                    });
        });
        return taintFlows;
    }
}
