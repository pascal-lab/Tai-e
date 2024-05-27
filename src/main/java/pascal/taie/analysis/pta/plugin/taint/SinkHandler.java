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
import pascal.taie.analysis.pta.core.cs.element.*;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.Canonicalizer;
import pascal.taie.util.collection.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handles sinks in taint analysis.
 */
class SinkHandler extends Handler {

    private final List<Sink> sinks;

    private final CSManager csManager;

    private final HeapModel heapModel;

    private final PointerToSetManager ptsManager = new PointerToSetManager();

    SinkHandler(HandlerContext context) {
        super(context);
        sinks = context.config().sinks();
        csManager = context.solver().getCSManager();
        heapModel = context.solver().getHeapModel();
    }

    Set<TaintFlow> collectTaintFlows() {
        PointerAnalysisResult result = solver.getResult();
        Set<TaintFlow> taintFlows = Sets.newOrderedSet();
        for (Sink sink : sinks) {
            result.getCallGraph()
                    .edgesInTo(sink.method())
                    // TODO: handle other call edges
                    .filter(e -> e.getKind() != CallKind.OTHER)
                    .map(Edge::getCallSite)
                    .map(sinkCall -> collectTaintFlows(sinkCall, sink))
                    .forEach(taintFlows::addAll);
        }
        if (callSiteMode) {
            MultiMap<JMethod, Sink> sinkMap = sinks.stream()
                    .collect(MultiMapCollector.get(Sink::method, s -> s));
            // scan all reachable call sites to search sink calls
            result.getCallGraph()
                    .reachableMethods()
                    .flatMap(m -> m.getIR().invokes(false))
                    .forEach(callSite -> {
                        JMethod callee = callSite.getMethodRef().resolveNullable();
                        if (callee != null) {
                            for (Sink sink : sinkMap.get(callee)) {
                                taintFlows.addAll(collectTaintFlows(callSite, sink));
                            }
                        }
                    });
        }
        return taintFlows;
    }

    private Set<TaintFlow> collectTaintFlows(
            Invoke sinkCall, Sink sink) {
        IndexRef indexRef = sink.indexRef();
        Var arg = InvokeUtils.getVar(sinkCall, indexRef.index());
        SinkPoint sinkPoint = new SinkPoint(sinkCall, indexRef);
        // obtain objects to check for different IndexRef.Kind
        Set<Obj> objs = switch (indexRef.kind()) {
            case VAR -> ptsManager.getPointsToSet(arg);
            case ARRAY -> ptsManager.getPointsToSet(arg, (Var) null);
            case FIELD -> ptsManager.getPointsToSet(arg, indexRef.field());
        };
        return objs.stream()
                .filter(manager::isTaint)
                .map(manager::getSourcePoint)
                .map(sourcePoint -> new TaintFlow(sourcePoint, sinkPoint))
                .collect(Collectors.toSet());
    }

    class PointerToSetManager {
        // todo: don't repeat urself!

        private static final Canonicalizer<Set<Obj>> canonicalizer = new Canonicalizer<>();

        private Set<Obj> removeContexts(Stream<CSObj> objects) {
            Set<Obj> set = new HybridBitSet<>(heapModel, true);
            objects.map(CSObj::getObject).forEach(set::add);
            return canonicalizer.get(Collections.unmodifiableSet(set));
        }

        Set<Obj> getPointsToSet(Var var) {
            return removeContexts(csManager.getCSVarsOf(var)
                    .stream()
                    .flatMap(Pointer::objects));
        }

        Set<Obj> getPointsToSet(Var base, JField field) {
            return removeContexts(csManager.getCSVarsOf(base)
                    .stream()
                    .flatMap(Pointer::objects)
                    .map(o -> csManager.getInstanceField(o, field))
                    .flatMap(InstanceField::objects));
        }

        Set<Obj> getPointsToSet(Var base, Var index) {
            return removeContexts(csManager.getCSVarsOf(base)
                    .stream()
                    .flatMap(Pointer::objects)
                    .map(csManager::getArrayIndex)
                    .flatMap(ArrayIndex::objects));
        }

    }
}
