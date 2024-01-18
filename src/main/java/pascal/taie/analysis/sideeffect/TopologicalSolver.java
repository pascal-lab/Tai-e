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

package pascal.taie.analysis.sideeffect;

import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.exp.FieldAccess;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.Indexer;
import pascal.taie.util.collection.CollectionUtils;
import pascal.taie.util.collection.IndexerBitSet;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.graph.MergedNode;
import pascal.taie.util.graph.MergedSCCGraph;
import pascal.taie.util.graph.TopologicalSorter;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Computes modification information based on pointer analysis
 * and topological sorting of call graph.
 */
class TopologicalSolver {

    private final boolean onlyApp;

    TopologicalSolver(boolean onlyApp) {
        this.onlyApp = onlyApp;
    }

    SideEffect solve(PointerAnalysisResult pta) {
        CallGraph<Invoke, JMethod> callGraph = pta.getCallGraph();
        // 1. compute the objects directly modified by each method and stmt
        Map<JMethod, Set<Obj>> methodDirectMods = Maps.newMap();
        Map<Stmt, Set<Obj>> stmtDirectMods = Maps.newMap();
        computeDirectMods(pta, callGraph, stmtDirectMods, methodDirectMods);
        // 2. compute the objects directly modified by
        //    the methods of each SCC in the call graph
        var mg = new MergedSCCGraph<>(callGraph);
        Map<JMethod, Set<Obj>> sccDirectMods = computeSCCDirectMods(
                mg.getNodes(), methodDirectMods);
        // 3. fully compute the objects modified by each method
        Indexer<Obj> indexer = pta.getObjectIndexer();
        Map<JMethod, Set<Obj>> methodMods = computeMethodMods(
                mg, callGraph, sccDirectMods, indexer);
        return new SideEffect(methodMods, stmtDirectMods, callGraph);
    }

    private void computeDirectMods(
            PointerAnalysisResult pta,
            CallGraph<?, JMethod> callGraph,
            Map<Stmt, Set<Obj>> stmtDirectMods,
            Map<JMethod, Set<Obj>> methodDirectMods) {
        callGraph.forEach(method -> {
            Set<Obj> mMods = Sets.newHybridSet();
            method.getIR().forEach(stmt -> {
                Set<Obj> sMods = Set.of();
                if (stmt instanceof StoreField storeField) {
                    FieldAccess fieldAccess = storeField.getFieldAccess();
                    if (fieldAccess instanceof InstanceFieldAccess instAccess) {
                        Var base = instAccess.getBase();
                        sMods = pta.getPointsToSet(base);
                    }
                } else if (stmt instanceof StoreArray storeArray) {
                    Var base = storeArray.getArrayAccess().getBase();
                    sMods = pta.getPointsToSet(base);
                }
                if (!sMods.isEmpty()) {
                    sMods = sMods.stream()
                            .filter(this::isRelevant)
                            .collect(Collectors.toUnmodifiableSet());
                }
                if (!sMods.isEmpty()) {
                    mMods.addAll(sMods);
                    stmtDirectMods.put(stmt, sMods);
                }
            });
            if (!mMods.isEmpty()) {
                methodDirectMods.put(method, mMods);
            }
        });
    }

    private boolean isRelevant(Obj obj) {
        if (onlyApp && obj.getContainerMethod().isPresent()) {
            return obj.getContainerMethod().get().isApplication();
        }
        return false;
    }

    private static Map<JMethod, Set<Obj>> computeSCCDirectMods(
            Set<MergedNode<JMethod>> sccs,
            Map<JMethod, Set<Obj>> methodDirectMods) {
        Map<JMethod, Set<Obj>> sccDirectMods = Maps.newMap();
        sccs.forEach(scc -> {
            Set<Obj> mods = Sets.newHybridSet();
            scc.getNodes().forEach(m ->
                    mods.addAll(methodDirectMods.getOrDefault(m, Set.of())));
            scc.getNodes().forEach(m -> sccDirectMods.put(m, mods));
        });
        return sccDirectMods;
    }

    private static Map<JMethod, Set<Obj>> computeMethodMods(
            MergedSCCGraph<JMethod> mg,
            CallGraph<?, JMethod> callGraph,
            Map<JMethod, Set<Obj>> sccDirectMods,
            Indexer<Obj> indexer) {
        Map<JMethod, Set<Obj>> methodMods = Maps.newMap();
        // to accelerate side-effect analysis, we propagate modified objects
        // of methods (methodMods) based on topological sorting of call graph,
        // so that each method only needs to be processed once
        var sorter = new TopologicalSorter<>(mg, true);
        sorter.get().forEach(scc -> {
            Set<Obj> mods = new IndexerBitSet<>(indexer, true);
            // add SCC direct mods
            Set<JMethod> sccNodes = Sets.newSet(scc.getNodes());
            JMethod rep = CollectionUtils.getOne(sccNodes);
            mods.addAll(sccDirectMods.get(rep));
            // add callees' mods
            sccNodes.forEach(m -> callGraph.getCalleesOfM(m)
                    .stream()
                    // avoid redundantly adding SCC direct mods
                    .filter(callee -> !sccNodes.contains(callee))
                    .forEach(callee -> mods.addAll(
                            methodMods.getOrDefault(callee, Set.of()))));
            if (!mods.isEmpty()) {
                sccNodes.forEach(m -> methodMods.put(m, mods));
            }
        });
        return methodMods;
    }
}
