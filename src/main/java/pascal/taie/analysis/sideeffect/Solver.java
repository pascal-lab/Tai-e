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
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.exp.FieldAccess;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.Sets;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

class Solver {

    private static final String SIDE_EFFECT_DESC = "staticObj";
    private final PointerAnalysisResult ptaResult;

    private final CallGraph<CSCallSite, CSMethod> callGraph;

    private Map<JMethod, Set<Obj>> methodMods;

    private Map<Stmt, Set<Obj>> stmtMods;

    Solver(PointerAnalysisResult ptaResult) {
        this.ptaResult = ptaResult;
        this.callGraph = ptaResult.getCSCallGraph();
    }

    SideEffect solve() {
        methodMods = Maps.newMap();
        stmtMods = Maps.newMap();
        Queue<Pair<CSMethod, Set<Obj>>> workList = initialize();
        propagate(workList);
        return canonicalizeResult();
    }

    private Queue<Pair<CSMethod, Set<Obj>>> initialize() {
        // initializes work list, computes stmtMod of StoreField/Array
        Queue<Pair<CSMethod, Set<Obj>>> workList = new ArrayDeque<>();
        for (CSMethod method : callGraph) {
            JMethod m = method.getMethod();
            Set<Obj> mMod = Sets.newHybridSet();
            for (Stmt stmt : m.getIR()) {
                Set<Obj> stmtMod = null;
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
                if (stmtMod != null && !stmtMod.isEmpty()) {
                    stmtMods.put(stmt, stmtMod);
                    mMod.addAll(stmtMod);
                }
            }
            if (!mMod.isEmpty()) {
                workList.add(new Pair<>(method, mMod));
            }
        }
        return workList;
    }

    private void propagate(Queue<Pair<CSMethod, Set<Obj>>> workList) {
        while (!workList.isEmpty()) {
            Pair<CSMethod, Set<Obj>> entry = workList.poll();
            CSMethod m = entry.first();
            Set<Obj> Objs = entry.second();
            Set<Obj> methodDelta = addToMod(methodMods, m.getMethod(), Objs);
            if (!methodDelta.isEmpty()) {
                for (CSCallSite callSite : callGraph.getCallersOf(m)) {
                    Invoke invoke = callSite.getCallSite();
                    Set<Obj> invokeDelta = addToMod(stmtMods, invoke, methodDelta);
                    if (!invokeDelta.isEmpty()) {
                        if (isAnalyzable(invoke.getContainer()))
                            workList.add(new Pair<>(callSite.getContainer(), invokeDelta));
                    }
                }
            }
        }
    }

    private static <K> Set<Obj> addToMod(
            Map<K, Set<Obj>> map, K key, Set<Obj> Objs) {
        Set<Obj> mod = map.computeIfAbsent(key, unused -> Sets.newHybridSet());
        Set<Obj> delta = Sets.newHybridSet();
        Objs.forEach(o -> {
            if (mod.add(o)) {
                delta.add(o);
            }
        });
        return delta;
    }

    private SideEffect canonicalizeResult() {
        Map<Set<Obj>, Set<Obj>> cache = Maps.newMap();
        return new SideEffect(canonicalizeMap(methodMods, cache),
                canonicalizeMap(stmtMods, cache));
    }

    private static <K> Map<K, Set<Obj>> canonicalizeMap(
            Map<K, Set<Obj>> map, Map<Set<Obj>, Set<Obj>> cache) {
        Map<K, Set<Obj>> result = Maps.newMap(map.size());
        map.forEach((k, Objs) -> {
            if (!Objs.isEmpty()) {
                result.put(k, cache.computeIfAbsent(
                        Objs, Collections::unmodifiableSet));
            }
        });
        return result;
    }

    private boolean isAnalyzable(JMethod method) {
        return !method.isNative() && method.getDeclaringClass().isApplication();
    }
}
