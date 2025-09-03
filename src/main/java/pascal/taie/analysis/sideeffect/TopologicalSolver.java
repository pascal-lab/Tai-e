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
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.Indexer;
import pascal.taie.util.collection.CollectionUtils;
import pascal.taie.util.collection.IndexerBitSet;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.TwoKeyMap;
import pascal.taie.util.graph.MergedNode;
import pascal.taie.util.graph.MergedSCCGraph;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Computes modification information based on pointer analysis
 * and topological sorting of call graph.
 */
class TopologicalSolver {

    private final boolean onlyApp;

    private final PointerAnalysisResult pta;

    private final CallGraph<CSCallSite, CSMethod> callGraph;

    private final Indexer<Obj> objIndexer;

    TopologicalSolver(boolean onlyApp, PointerAnalysisResult pta) {
        this.onlyApp = onlyApp;
        this.pta = pta;
        this.callGraph = new CachedCallGraph<>(pta.getCSCallGraph());
        this.objIndexer = pta.getObjectIndexer();
    }

    SideEffect solve() {
        // 1. compute the objects directly modified by each method and stmt
        TwoKeyMap<JMethod, Context, Set<Obj>> methodDirectMods = Maps.newTwoKeyMap();
        TwoKeyMap<Stmt, Context, Set<Obj>> stmtDirectMods = Maps.newTwoKeyMap();
        computeDirectMods(stmtDirectMods, methodDirectMods);
        // 2. compute the objects directly modified by
        //    the methods of each SCC in the call graph
        var mg = new MergedSCCGraph<>(callGraph);
        TwoKeyMap<JMethod, Context, Set<Obj>> sccDirectMods = computeSCCDirectMods(
                mg.getNodes(), methodDirectMods);
        // 3. fully compute the objects modified by each method
        TwoKeyMap<JMethod, Context, Set<Obj>> methodMods = computeMethodMods(
                mg, sccDirectMods);
        return new SideEffect(methodMods, stmtDirectMods, pta.getCallGraph());
    }

    private void computeDirectMods(
            TwoKeyMap<Stmt, Context, Set<Obj>> stmtDirectMods,
            TwoKeyMap<JMethod, Context, Set<Obj>> methodDirectMods) {
        for (CSVar csVar : pta.getCSVars()) {
            Context context = csVar.getContext();
            Var base = csVar.getVar();
            JMethod method = base.getMethod();
            Set<Obj> baseObjs = csVar.objects()
                    .map(CSObj::getObject)
                    .filter(this::isRelevant)
                    .collect(Collectors.toUnmodifiableSet());
            if (!base.getStoreFields().isEmpty()) {
                for (Stmt stmt : base.getStoreFields()) {
                    stmtDirectMods.computeIfAbsent(stmt, context,
                                    (s, c) -> Sets.newSet())
                            .addAll(baseObjs);
                }
                methodDirectMods.computeIfAbsent(method, context,
                                (m, c) -> Sets.newSet())
                        .addAll(baseObjs);
            }
            if (!base.getStoreArrays().isEmpty()) {
                for (Stmt stmt : base.getStoreArrays()) {
                    stmtDirectMods.computeIfAbsent(stmt, context,
                                    (s, c) -> Sets.newSet())
                            .addAll(baseObjs);
                }
                methodDirectMods.computeIfAbsent(method, context,
                                (m, c) -> Sets.newSet())
                        .addAll(baseObjs);
            }
        }
    }

    private boolean isRelevant(Obj obj) {
        // TODO: this method might not be well-defined for MergedObjs
        if (onlyApp) {
            return obj.getContainerMethod().isPresent()
                    && obj.getContainerMethod().get().isApplication();
        }
        return true;
    }

    private static TwoKeyMap<JMethod, Context, Set<Obj>> computeSCCDirectMods(
            Set<MergedNode<CSMethod>> sccs,
            TwoKeyMap<JMethod, Context, Set<Obj>> methodDirectMods) {
        TwoKeyMap<JMethod, Context, Set<Obj>> sccDirectMods = Maps.newTwoKeyMap();
        for (MergedNode<CSMethod> scc : sccs) {
            Set<Obj> mods = Sets.newHybridSet();
            for (CSMethod csMethod : scc.getNodes()) {
                Context context = csMethod.getContext();
                JMethod method = csMethod.getMethod();
                mods.addAll(methodDirectMods.getOrDefault(method, context, Set.of()));
            }
            for (CSMethod csMethod : scc.getNodes()) {
                Context context = csMethod.getContext();
                JMethod method = csMethod.getMethod();
                sccDirectMods.put(method, context, mods);
            }
        }
        return sccDirectMods;
    }

    private TwoKeyMap<JMethod, Context, Set<Obj>> computeMethodMods(
            MergedSCCGraph<CSMethod> mg,
            TwoKeyMap<JMethod, Context, Set<Obj>> sccDirectMods) {
        TwoKeyMap<JMethod, Context, Set<Obj>> methodMods = Maps.newTwoKeyMap();
        // to accelerate side effect analysis, we propagate modified objects
        // of methods (methodMods) based on topological sorting of call graph,
        // so that each method only needs to be processed once
        var sorter = new TopologicalSorter<>(mg, true);
        for (MergedNode<CSMethod> scc : sorter.get()) {
            Set<Obj> mods = new IndexerBitSet<>(objIndexer, true);
            // add SCC direct mods
            Set<CSMethod> sccNodes = Sets.newSet(scc.getNodes());
            CSMethod rep = CollectionUtils.getOne(sccNodes);
            Context repContext = rep.getContext();
            JMethod repMethod = rep.getMethod();
            mods.addAll(sccDirectMods.getOrDefault(repMethod, repContext, Set.of()));
            // add callees' mods
            sccNodes.stream().map(callGraph::getCalleesOfM).flatMap(Collection::stream)
                    .distinct()
                    // avoid redundantly adding SCC direct mods
                    .filter(callee -> !sccNodes.contains(callee))
                    .forEach(csCallee -> {
                        Context context = csCallee.getContext();
                        JMethod callee = csCallee.getMethod();
                        mods.addAll(methodMods.getOrDefault(callee, context, Set.of()));
                    });
            if (!mods.isEmpty()) {
                for (CSMethod csMethod : sccNodes) {
                    Context context = csMethod.getContext();
                    JMethod method = csMethod.getMethod();
                    methodMods.put(method, context, mods);
                }
            }
        }
        return methodMods;
    }
}
