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
import pascal.taie.util.collection.CollectionUtils;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.graph.MergedNode;
import pascal.taie.util.graph.MergedSCCGraph;
import pascal.taie.util.graph.TopoSorter;

import java.util.Map;
import java.util.Set;

class TopologicalSolver {

    private static final String SIDE_EFFECT_DESC = "staticObj";
    private final PointerAnalysisResult ptaResult;

    private final CallGraph<Invoke, JMethod> callGraph;

    private Map<JMethod, Set<Obj>> methodMods;

    private Map<Stmt, Set<Obj>> stmtMods;

    TopologicalSolver(PointerAnalysisResult ptaResult) {
        this.ptaResult = ptaResult;
        this.callGraph = ptaResult.getCallGraph();
    }

    SideEffect solve() {
        methodMods = Maps.newMap();
        stmtMods = Maps.newMap();
        // compute method mods of each individual method itself
        Map<JMethod, Set<Obj>> selfMods = computeSelfMMods(ptaResult);
        Map<JMethod, Set<Obj>> sccMods = Maps.newMap();
        MergedSCCGraph<JMethod> mg = new MergedSCCGraph<>(callGraph);
        TopoSorter<MergedNode<JMethod>> sorter = new TopoSorter<>(mg, true);
        // compute method mods of each SCC in the call graph
        sorter.get().forEach(scc -> {
            Set<Obj> mods = Sets.newHybridSet();
            scc.getNodes().forEach(m -> mods.addAll(selfMods.get(m)));
            scc.getNodes().forEach(m -> sccMods.put(m, mods));
        });
        // propagate method mods along call graph
        sorter.get().forEach(scc -> {
            Set<JMethod> sccNodes = Sets.newSet(scc.getNodes());
            // add SCC mods
            JMethod rep = CollectionUtils.getOne(sccNodes);
            Set<Obj> modifiedObjs = Sets.newHybridSet(sccMods.get(rep));
            // add callee mods
            sccNodes.forEach(m -> {
                callGraph.getCalleesOfM(m)
                        .stream()
                        .filter(callee -> !sccNodes.contains(callee))
                        .forEach(callee -> modifiedObjs.addAll(methodMods.get(callee)));
            });
            sccNodes.forEach(m -> methodMods.put(m, modifiedObjs));
        });
        return new SideEffect(methodMods, stmtMods, callGraph);
    }

    private Map<JMethod, Set<Obj>> computeSelfMMods(
            PointerAnalysisResult ptaResult) {
        CallGraph<?, JMethod> callGraph = ptaResult.getCallGraph();
        Map<JMethod, Set<Obj>> selfMMods = Maps.newMap();
        callGraph.forEach(method -> {
            Set<Obj> mMod = Sets.newHybridSet();
            method.getIR().forEach(stmt -> {
                Set<Obj> stmtMod = Set.of();
                if (stmt instanceof StoreField storeField) {
                    FieldAccess fieldAccess = storeField.getFieldAccess();
                    if (fieldAccess instanceof InstanceFieldAccess instAccess) {
                        Var base = instAccess.getBase();
                        stmtMod = ptaResult.getPointsToSet(base);
                    }
                } else if (stmt instanceof StoreArray storeArray) {
                    Var base = storeArray.getArrayAccess().getBase();
                    stmtMod = ptaResult.getPointsToSet(base);
                }
                if (!stmtMod.isEmpty()) {
                    mMod.addAll(stmtMod);
                    stmtMods.put(stmt, stmtMod);
                }
            });
            selfMMods.put(method, mMod);
        });
        return selfMMods;
    }
}
