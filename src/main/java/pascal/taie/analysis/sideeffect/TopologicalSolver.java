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
import pascal.taie.util.graph.Graph;
import pascal.taie.util.graph.MergedNode;
import pascal.taie.util.graph.MergedSCCGraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Computes side-effect information (which objects might be modified) based on pointer analysis
 * and topological sorting of context-sensitive call graph.
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
        // Step 1: Compute the objects directly modified by each statement and method
        // Based on rules: x.f = y -> modify(stmt/method) += pts(x)
        //                 x[*] = y -> modify(stmt/method) += pts(x)
        TwoKeyMap<Stmt, Context, Set<Obj>> stmtDirectMods = Maps.newTwoKeyMap();
        TwoKeyMap<JMethod, Context, Set<Obj>> methodDirectMods = Maps.newTwoKeyMap();
        computeDirectMods(stmtDirectMods, methodDirectMods);

        // Step 2: Merge modified objects within each SCC (Strongly Connected Component)
        // Methods in the same SCC can call each other recursively
        MergedSCCGraph<CSMethod> mg = new MergedSCCGraph<>(callGraph);
        TwoKeyMap<JMethod, Context, Set<Obj>> sccDirectMods = computeSCCDirectMods(
                mg.getNodes(), methodDirectMods);

        // Step 3: Propagate modified objects through call graph in reverse topological order
        // Based on rule: x = y.foo(...) -> modify(caller) += modify(callee)
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

            // Handle field store
            if (!base.getStoreFields().isEmpty()) {
                for (Stmt stmt : base.getStoreFields()) {
                    // Rule: x.f = y, o ∈ pts(x) => o ∈ modify(stmt)
                    stmtDirectMods.computeIfAbsent(stmt, context,
                                    (s, c) -> Sets.newSet())
                            .addAll(baseObjs);
                }
                // Rule: x.f = y, o ∈ pts(x) => o ∈ modify(method)
                methodDirectMods.computeIfAbsent(method, context,
                                (m, c) -> Sets.newSet())
                        .addAll(baseObjs);
            }

            // Handle array store
            if (!base.getStoreArrays().isEmpty()) {
                for (Stmt stmt : base.getStoreArrays()) {
                    // Rule: x[*] = y, o ∈ pts(x) => o ∈ modify(stmt)
                    stmtDirectMods.computeIfAbsent(stmt, context,
                                    (s, c) -> Sets.newSet())
                            .addAll(baseObjs);
                }
                // Rule: x[*] = y, o ∈ pts(x) => o ∈ modify(method)
                methodDirectMods.computeIfAbsent(method, context,
                                (m, c) -> Sets.newSet())
                        .addAll(baseObjs);
            }
        }
    }

    /**
     * Filters objects based on analysis scope.
     * If onlyApp is true, only considers objects from application methods.
     */
    private boolean isRelevant(Obj obj) {
        // TODO: this method might not be well-defined for MergedObjs
        if (onlyApp) {
            return obj.getContainerMethod().isPresent()
                    && obj.getContainerMethod().get().isApplication();
        }
        return true;
    }

    /**
     * Computes objects directly modified by methods in each SCC.
     * All methods in the same SCC share the same set of directly modified objects
     * because they can call each other recursively.
     */
    private static TwoKeyMap<JMethod, Context, Set<Obj>> computeSCCDirectMods(
            Set<MergedNode<CSMethod>> sccs,
            TwoKeyMap<JMethod, Context, Set<Obj>> methodDirectMods) {
        TwoKeyMap<JMethod, Context, Set<Obj>> sccDirectMods = Maps.newTwoKeyMap();
        for (MergedNode<CSMethod> scc : sccs) {
            // Collect all directly modified objects within the SCC
            Set<Obj> mods = Sets.newHybridSet();
            for (CSMethod csMethod : scc.getNodes()) {
                Context context = csMethod.getContext();
                JMethod method = csMethod.getMethod();
                mods.addAll(methodDirectMods.getOrDefault(method, context, Set.of()));
            }

            // Assign the collected modified objects to all methods in the SCC
            for (CSMethod csMethod : scc.getNodes()) {
                Context context = csMethod.getContext();
                JMethod method = csMethod.getMethod();
                sccDirectMods.put(method, context, mods);
            }
        }
        return sccDirectMods;
    }

    /**
     * Computes final side-effect for each method by propagating
     * modified objects through the call graph.
     * Uses reverse topological order to ensure each method is processed only once.
     */
    private TwoKeyMap<JMethod, Context, Set<Obj>> computeMethodMods(
            MergedSCCGraph<CSMethod> mg,
            TwoKeyMap<JMethod, Context, Set<Obj>> sccDirectMods) {
        TwoKeyMap<JMethod, Context, Set<Obj>> methodMods = Maps.newTwoKeyMap();

        // Process SCCs in reverse topological order
        // This ensures that when processing an SCC, all its callees have been processed
        for (MergedNode<CSMethod> scc : reverseTopologicalSort(mg)) {
            Set<Obj> mods = new IndexerBitSet<>(objIndexer, true);

            // Pick a representative method from the SCC
            Set<CSMethod> sccNodes = Sets.newSet(scc.getNodes());
            CSMethod rep = CollectionUtils.getOne(sccNodes);
            Context repContext = rep.getContext();
            JMethod repMethod = rep.getMethod();

            // Add objects that are modified by methods in the same SCC
            mods.addAll(sccDirectMods.getOrDefault(repMethod, repContext, Set.of()));

            // Add modified objects from callees
            // Rule: i: x = y.foo(...) ->call j, j ∈ m, o ∈ modify(j) => o ∈ modify(i)
            sccNodes.stream().map(callGraph::getCalleesOfM).flatMap(Collection::stream)
                    .distinct()
                    // Avoid redundantly adding SCC direct mods (already added above)
                    .filter(callee -> !sccNodes.contains(callee))
                    .forEach(csCallee -> {
                        Context context = csCallee.getContext();
                        JMethod callee = csCallee.getMethod();
                        mods.addAll(methodMods.getOrDefault(callee, context, Set.of()));
                    });

            // Assign computed side-effect to all methods in the SCC
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

    /**
     * Performs reverse topological sorting on a directed acyclic graph (DAG) using BFS.
     * This implementation is optimized for side-effect analysis by avoiding redundant traversals.
     * <p>
     * The reverse topological order ensures that when processing a node,
     * all its successors (callees) have already been processed.
     *
     * @param <N>   type of nodes in the graph
     * @param graph the graph to sort
     * @return list of nodes in reverse topological order
     */
    private static <N> List<N> reverseTopologicalSort(Graph<N> graph) {
        // Map to track in-degree of each node
        Map<N, Long> inDegreeMap = Maps.newMap();
        Queue<N> queue = new ArrayDeque<>();
        for (N node : graph.getNodes()) {
            long inDegree = graph.getInDegreeOf(node);
            inDegreeMap.put(node, inDegree);
            if (inDegree == 0) {
                queue.add(node);
            }
        }

        List<N> result = new ArrayList<>(graph.getNumberOfNodes());
        while (!queue.isEmpty()) {
            N curr = queue.poll();
            result.add(curr);
            for (N pred : graph.getSuccsOf(curr)) {
                long inDegree = inDegreeMap.get(pred) - 1;
                inDegreeMap.put(pred, inDegree);
                if (inDegree == 0) {
                    queue.add(pred);
                }
            }
        }

        // Reverse to get reverse topological order
        Collections.reverse(result);
        return Collections.unmodifiableList(result);
    }
}
