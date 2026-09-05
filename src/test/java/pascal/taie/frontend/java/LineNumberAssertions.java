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

package pascal.taie.frontend.java;

import pascal.taie.ir.exp.CastExp;
import pascal.taie.ir.exp.InstanceFieldAccess;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.NullLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.AssignLiteral;
import pascal.taie.ir.stmt.Binary;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.If;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.Monitor;
import pascal.taie.ir.stmt.Nop;
import pascal.taie.ir.stmt.Phi;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.language.classes.JMethod;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Shared semantic assertions for Java IR line-number tests. */
final class LineNumberAssertions {

    private LineNumberAssertions() {
    }

    static void assertInstanceFieldCastLine(JMethod method,
            String fieldName, int expectedLine) {
        List<LoadField> loads = method.getIR().stmts()
                .filter(LoadField.class::isInstance)
                .map(LoadField.class::cast)
                .filter(stmt -> stmt.getFieldRef().getName().equals(fieldName))
                .toList();
        assertEquals(1, loads.size(), "loads of " + fieldName + " in " + method);
        LoadField load = loads.get(0);
        InstanceFieldAccess access = (InstanceFieldAccess) load.getFieldAccess();
        List<Cast> casts = method.getIR().stmts()
                .filter(Cast.class::isInstance)
                .map(Cast.class::cast)
                .toList();
        assertEquals(1, casts.size(), "casts in " + method);
        Cast cast = casts.get(0);
        assertTrue(cast.getLValue() == access.getBase(),
                "receiver cast does not feed " + load);
        CastExp castExp = cast.getRValue();
        assertTrue(method.getIR().stmts()
                        .anyMatch(stmt -> stmt.getDef().orElse(null)
                                == castExp.getValue()),
                "cast source is undefined: " + cast);
        assertLine(cast, expectedLine);
        assertLine(load, expectedLine);
    }

    static void assertInferredCastLine(JMethod method, int expectedLine) {
        List<Cast> casts = method.getIR().stmts()
                .filter(Cast.class::isInstance)
                .map(Cast.class::cast)
                .toList();
        assertEquals(1, casts.size(), "inferred casts in " + method);
        assertLine(casts.get(0), expectedLine);
    }

    static void assertCastLines(JMethod method, List<Integer> expectedLines) {
        List<Integer> actualLines = method.getIR().stmts()
                .filter(Cast.class::isInstance)
                .map(Stmt::getLineNumber)
                .toList();
        assertEquals(expectedLines, actualLines, "casts in " + method);
    }

    static void assertStoreFieldLine(JMethod method,
            String fieldName, int expectedLine) {
        List<StoreField> stores = method.getIR().stmts()
                .filter(StoreField.class::isInstance)
                .map(StoreField.class::cast)
                .filter(stmt -> stmt.getFieldRef().getName().equals(fieldName))
                .toList();
        assertEquals(1, stores.size(), "stores of " + fieldName + " in " + method);
        assertLine(stores.get(0), expectedLine);
    }

    static void assertReturnLines(JMethod method, List<Integer> expectedLines) {
        assertLines(method, Return.class, expectedLines, "return statements");
    }

    static void assertReturnLine(JMethod method, int expectedLine) {
        List<Return> returns = method.getIR().stmts()
                .filter(Return.class::isInstance)
                .map(Return.class::cast)
                .filter(stmt -> stmt.getValue() != null)
                .toList();
        assertEquals(1, returns.size(), "value returns in " + method);
        assertLine(returns.get(0), expectedLine);
    }

    static void assertReturnLiteralLines(JMethod method,
            int literalValue, List<Integer> expectedLines) {
        List<Integer> actualLines = method.getIR().stmts()
                .filter(Return.class::isInstance)
                .map(Return.class::cast)
                .filter(stmt -> stmt.getValue() != null)
                .filter(stmt -> stmt.getValue().isConst())
                .filter(stmt -> stmt.getValue().getConstValue() instanceof IntLiteral literal
                        && literal.getValue() == literalValue)
                .map(Stmt::getLineNumber)
                .toList();
        assertEquals(expectedLines, actualLines,
                "returns of " + literalValue + " in " + method);
    }

    static void assertLiteralDefinitionLine(JMethod method,
            int literalValue, int expectedLine) {
        List<AssignLiteral> assignments = method.getIR().stmts()
                .filter(AssignLiteral.class::isInstance)
                .map(AssignLiteral.class::cast)
                .filter(stmt -> stmt.getRValue() instanceof IntLiteral literal
                        && literal.getValue() == literalValue)
                .toList();
        assertEquals(1, assignments.size(),
                "literal definitions of " + literalValue + " in " + method);
        assertLine(assignments.get(0), expectedLine);
    }

    static void assertCopyLine(JMethod method, Var source, int expectedLine) {
        List<Copy> copies = method.getIR().stmts()
                .filter(Copy.class::isInstance)
                .map(Copy.class::cast)
                .filter(stmt -> stmt.getRValue() == source)
                .toList();
        assertEquals(1, copies.size(), "copies of " + source + " in " + method);
        assertLine(copies.get(0), expectedLine);
    }

    static void assertCopyLines(JMethod method,
            String variableName, List<Integer> expectedLines) {
        List<Integer> actualLines = method.getIR().stmts()
                .filter(Copy.class::isInstance)
                .map(Copy.class::cast)
                .filter(stmt -> stmt.getLValue().getName().equals(variableName))
                .map(Stmt::getLineNumber)
                .toList();
        assertEquals(expectedLines, actualLines,
                "copies assigned to " + variableName + " in " + method);
    }

    static void assertNullAssignmentLine(JMethod method, int expectedLine) {
        List<AssignLiteral> assignments = method.getIR().stmts()
                .filter(AssignLiteral.class::isInstance)
                .map(AssignLiteral.class::cast)
                .filter(stmt -> stmt.getRValue() == NullLiteral.get())
                .toList();
        assertEquals(1, assignments.size(), "null assignments in " + method);
        assertLine(assignments.get(0), expectedLine);
    }

    static void assertPhiLine(JMethod method,
            String variableName, int expectedLine) {
        List<Phi> phis = method.getIR().stmts()
                .filter(Phi.class::isInstance)
                .map(Phi.class::cast)
                .filter(stmt -> stmt.getLValue().getName().equals(variableName))
                .toList();
        assertEquals(1, phis.size(), "Phis for " + variableName + " in " + method);
        assertLine(phis.get(0), expectedLine);
    }

    static void assertLiteralLine(JMethod method,
            String variableName, int literalValue, int expectedLine) {
        List<AssignLiteral> assignments = method.getIR().stmts()
                .filter(AssignLiteral.class::isInstance)
                .map(AssignLiteral.class::cast)
                .filter(stmt -> stmt.getLValue().getName().equals(variableName))
                .filter(stmt -> stmt.getRValue() instanceof IntLiteral literal
                        && literal.getValue() == literalValue)
                .toList();
        assertEquals(1, assignments.size(),
                "assignments of " + variableName + " = " + literalValue
                        + " in " + method);
        assertLine(assignments.get(0), expectedLine);
    }

    static void assertBinaryLines(JMethod method, List<Integer> expectedLines) {
        assertLines(method, Binary.class, expectedLines, "binary statements");
    }

    static void assertIfLines(JMethod method, List<Integer> expectedLines) {
        assertLines(method, If.class, expectedLines, "if statements");
    }

    static void assertInvokeLines(JMethod method, List<Integer> expectedLines) {
        assertLines(method, Invoke.class, expectedLines, "invoke statements");
    }

    static void assertMonitorEnterLines(JMethod method,
            List<Integer> expectedLines) {
        List<Integer> actualLines = method.getIR().stmts()
                .filter(Monitor.class::isInstance)
                .map(Monitor.class::cast)
                .filter(Monitor::isEnter)
                .map(Stmt::getLineNumber)
                .toList();
        assertEquals(expectedLines, actualLines,
                "monitor enters in " + method);
    }

    static void assertMonitorExitLines(JMethod method,
            List<Integer> expectedLines) {
        List<Integer> actualLines = method.getIR().stmts()
                .filter(Monitor.class::isInstance)
                .map(Monitor.class::cast)
                .filter(Monitor::isExit)
                .map(Stmt::getLineNumber)
                .toList();
        assertEquals(expectedLines, actualLines,
                "monitor exits in " + method);
    }

    static void assertNopLine(JMethod method, int expectedLine) {
        assertLines(method, Nop.class, List.of(expectedLine), "nop statements");
    }

    static void assertAllLines(JMethod method, int expectedLine) {
        method.getIR().stmts().forEach(stmt -> assertLine(stmt, expectedLine));
    }

    static void assertLine(Stmt stmt, int expectedLine) {
        assertEquals(expectedLine, stmt.getLineNumber(), stmt.toString());
    }

    static void assertInvokeLine(JMethod method,
            String declaringClass, String methodName, int expectedLine) {
        List<Invoke> invokes = method.getIR().invokes(false)
                .filter(invoke -> invoke.getMethodRef().getDeclaringClass().getName()
                        .equals(declaringClass))
                .filter(invoke -> invoke.getMethodRef().getName().equals(methodName))
                .toList();
        assertEquals(1, invokes.size(),
                "invokes of " + declaringClass + "." + methodName
                        + " in " + method);
        assertLine(invokes.get(0), expectedLine);
    }

    private static void assertLines(JMethod method,
            Class<? extends Stmt> statementType, List<Integer> expectedLines,
            String description) {
        List<Integer> actualLines = method.getIR().stmts()
                .filter(statementType::isInstance)
                .map(Stmt::getLineNumber)
                .toList();
        assertEquals(expectedLines, actualLines, description + " in " + method);
    }
}
