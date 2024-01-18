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

package pascal.taie.analysis.pta.plugin.assertion;

import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.StringLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.CollectionUtils;
import pascal.taie.util.collection.Maps;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementations of {@link Checker}.
 */
enum Checkers {

    NOT_EMPTY("void notEmpty(java.lang.Object[])", (invoke, pta, __, ___) -> {
        List<Var> checkVars = getStoredVariables(invoke, 0);
        String assertion = String.format(
                "points-to sets of variables %s are not empty", checkVars);
        Map<Var, Set<Obj>> failures = Maps.newLinkedHashMap();
        checkVars.forEach(v -> {
            Set<Obj> pts = pta.getPointsToSet(v);
            if (pts.isEmpty()) {
                failures.put(v, pts);
            }
        });
        return new Result(invoke, assertion, failures);
    }),
    SIZE_EQUALS("void sizeEquals(int,java.lang.Object[])", (invoke, pta, __, ___) -> {
        int size = getInt(InvokeUtils.getVar(invoke, 0));
        List<Var> checkVars = getStoredVariables(invoke, 1);
        String assertion = String.format(
                "size of points-to sets of variables %s is %d", checkVars, size);
        Map<Var, Set<Obj>> failures = Maps.newLinkedHashMap();
        checkVars.forEach(v -> {
            Set<Obj> pts = pta.getPointsToSet(v);
            if (pts.size() != size) {
                failures.put(v, pts);
            }
        });
        return new Result(invoke, assertion, failures);
    }),
    EQUALS("void equals(java.lang.Object[])", (invoke, pta, __, ___) -> {
        List<Var> checkVars = getStoredVariables(invoke, 0);
        String assertion = String.format(
                "points-to sets of variables %s are equal", checkVars);
        Set<Obj> pts = pta.getPointsToSet(CollectionUtils.getOne(checkVars));
        Map<Var, Set<Obj>> failures = Maps.newLinkedHashMap();
        if (!checkVars.stream()
                .map(pta::getPointsToSet)
                .allMatch(pts::equals)) {
            checkVars.forEach(v -> failures.put(v, pta.getPointsToSet(v)));
        }
        return new Result(invoke, assertion, failures);
    }),
    CONTAINS("void contains(java.lang.Object,java.lang.Object[])", (invoke, pta, __, ___) -> {
        Var x = InvokeUtils.getVar(invoke, 0);
        List<Var> checkVars = getStoredVariables(invoke, 1);
        String assertion = String.format(
                "pt(%s) contains points-to sets of variables %s", x, checkVars);
        Set<Obj> xPts = pta.getPointsToSet(x);
        Map<Var, Set<Obj>> failures = Maps.newLinkedHashMap();
        checkVars.forEach(v -> {
            Set<Obj> vPts = pta.getPointsToSet(v);
            if (!xPts.containsAll(vPts)) {
                failures.put(x, xPts);
                failures.put(v, vPts);
            }
        });
        return new Result(invoke, assertion, failures);
    }),
    INSTANCEOF_IN("void instanceOfIn(java.lang.String,java.lang.Object[])", (invoke, pta, __, typeSystem) -> {
        String typeName = getString(InvokeUtils.getVar(invoke, 0));
        Type expected = typeSystem.getType(typeName);
        List<Var> checkVars = getStoredVariables(invoke, 1);
        String assertion = String.format(
                "points-to sets of variables %s has instance of %s", checkVars, expected);
        Map<Var, Set<Obj>> failures = Maps.newLinkedHashMap();
        checkVars.forEach(v -> {
            Set<Obj> pts = pta.getPointsToSet(v);
            if (pts.stream()
                    .map(Obj::getType)
                    .noneMatch(actual -> typeSystem.isSubtype(expected, actual))) {
                failures.put(v, pts);
            }
        });
        return new Result(invoke, assertion, failures);
    }),
    HAS_INSTANCEOF("void hasInstanceOf(java.lang.Object,java.lang.String[])", (invoke, pta, __, typeSystem) -> {
        Var x = InvokeUtils.getVar(invoke, 0);
        List<Type> expectedTypes = getStoredVariables(invoke, 1)
                .stream()
                .map(v -> typeSystem.getType(getString(v)))
                .toList();
        String assertion = String.format(
                "pt(%s) has instances of %s", x, expectedTypes);
        Set<Obj> pts = pta.getPointsToSet(x);
        Set<Type> actualTypes = pts.stream()
                .map(Obj::getType)
                .collect(Collectors.toUnmodifiableSet());
        Map<Var, Set<Obj>> failures = Maps.newHybridMap();
        for (Type expected : expectedTypes) {
            if (actualTypes.stream()
                    .noneMatch(actual -> typeSystem.isSubtype(expected, actual))) {
                failures.put(x, pts);
                break;
            }
        }
        return new Result(invoke, assertion, failures);
    }),
    NOT_EQUALS("void notEquals(java.lang.Object,java.lang.Object)", (invoke, pta, __, ___) -> {
        Var x = InvokeUtils.getVar(invoke, 0);
        Var y = InvokeUtils.getVar(invoke, 1);
        String assertion = String.format("pt(%s) != pt(%s)", x, y);
        Map<Var, Set<Obj>> failures = Maps.newLinkedHashMap();
        Set<Obj> xPts = pta.getPointsToSet(x), yPts = pta.getPointsToSet(y);
        if (xPts.equals(yPts)) {
            failures.put(x, xPts);
            failures.put(y, yPts);
        }
        return new Result(invoke, assertion, failures);
    }),
    DISJOINT("void disjoint(java.lang.Object,java.lang.Object)", (invoke, pta, __, ___) -> {
        Var x = InvokeUtils.getVar(invoke, 0);
        Var y = InvokeUtils.getVar(invoke, 1);
        String assertion = String.format("pt(%s) ^ pt(%s) = {}", x, y);
        Map<Var, Set<Obj>> failures = Maps.newLinkedHashMap();
        Set<Obj> xPts = pta.getPointsToSet(x), yPts = pta.getPointsToSet(y);
        if (!Collections.disjoint(xPts, yPts)) {
            failures.put(x, xPts);
            failures.put(y, yPts);
        }
        return new Result(invoke, assertion, failures);
    }),
    CALLS("void calls(java.lang.String[])", (invoke, pta, hierarchy, ___)  -> {
        Invoke callSite = findCallSiteBefore(invoke);
        List<JMethod> expectedCallees = getStoredVariables(invoke, 0)
                .stream()
                .map(v -> hierarchy.getMethod(getString(v)))
                .toList();
        String assertion = String.format("%s calls %s", callSite, expectedCallees);
        Set<JMethod> actualCallees = pta.getCallGraph().getCalleesOf(callSite);
        Map<Invoke, Set<JMethod>> failures = actualCallees.containsAll(expectedCallees)
                ? Map.of()
                : Map.of(callSite, actualCallees);
        return new Result(invoke, assertion, failures);
    }),
    CALLS_EXACT("void callsExact(java.lang.String[])", (invoke, pta, hierarchy, ___)  -> {
        Invoke callSite = findCallSiteBefore(invoke);
        Set<JMethod> expectedCallees = getStoredVariables(invoke, 0)
                .stream()
                .map(v -> hierarchy.getMethod(getString(v)))
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
        String assertion = String.format("%s calls exactly %s", callSite, expectedCallees);
        Set<JMethod> actualCallees = pta.getCallGraph().getCalleesOf(callSite);
        Map<Invoke, Set<JMethod>> failures = actualCallees.equals(expectedCallees)
                ? Map.of()
                : Map.of(callSite, actualCallees);
        return new Result(invoke, assertion, failures);
    }),
    REACHABLE("void reachable(java.lang.String[])", (invoke, pta, hierarchy, ___)  -> {
        List<JMethod> reachable = getStoredVariables(invoke, 0)
                .stream()
                .map(v -> hierarchy.getMethod(getString(v)))
                .toList();
        String assertion = String.format("%s are reachable", reachable);
        Map<JMethod, String> failures = Maps.newLinkedHashMap();
        reachable.forEach(method -> {
            if (!pta.getCallGraph().contains(method)) {
                failures.put(method, "unreachable");
            }
        });
        return new Result(invoke, assertion, failures);
    }),
    ;

    private final String api;

    private final Checker checker;

    Checkers(String subsig, Checker checker) {
        this.api = String.format("<%s: %s>", AssertionChecker.PTA_ASSERT, subsig);
        this.checker = checker;
    }

    String getApi() {
        return api;
    }

    Checker getChecker() {
        return checker;
    }

    private static List<Var> getStoredVariables(Invoke invoke, int index) {
        Var array = InvokeUtils.getVar(invoke, index);
        return invoke.getContainer().getIR()
                .stmts()
                .filter(s -> s instanceof StoreArray store
                        && store.getArrayAccess().getBase().equals(array))
                .map(s -> ((StoreArray) s).getRValue())
                .toList();
    }

    private static int getInt(Var var) {
        return ((IntLiteral) var.getConstValue()).getValue();
    }

    private static String getString(Var var) {
        return ((StringLiteral) var.getConstValue()).getString();
    }

    private static Invoke findCallSiteBefore(Invoke invoke) {
        IR ir = invoke.getContainer().getIR();
        for (int i = invoke.getIndex() - 1; i >= 0; --i) {
            if (ir.getStmt(i) instanceof Invoke callSite) {
                return callSite;
            }
        }
        throw new RuntimeException("No call site before " + invoke);
    }
}
