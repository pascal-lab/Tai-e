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

/**
 * Stub classes that provides assertion APIs.
 * Invocations of these APIs are treated as assertions and would be checked by
 * {@link pascal.taie.analysis.pta.plugin.assertion.AssertionChecker}.
 */
public final class PTAAssert {

    /**
     * Asserts that for all vi in vars: pt(v0) != {}, pt(v1) != {} ...
     */
    public static void notEmpty(Object... vars) {
    }

    /**
     * Asserts that for all vi in vars: |pt(v0)| = |pt(v1)| = ... = expected.
     * NOTE: {@code expected} must be an integer constant.
     */
    public static void sizeEquals(int expected, Object... vars) {
    }

    /**
     * Asserts that for all vi in vars: pt(v0) = pt(v1) = ...
     */
    public static void equals(Object... vars) {
    }

    /**
     * Asserts that for all vi in vars: pt(x) contains pt(v0), pt(x) contains pt(v1) ...
     */
    public static void contains(Object x, Object... vars) {
    }

    /**
     * Asserts that for all vi in vars: pt(vi) contains objects of specified class.
     * NOTE: {@code className} must be a String constant.
     */
    public static void instanceOfIn(String className, Object... vars) {
    }

    /**
     * Asserts that pt(x) contains objects of specified classes.
     * NOTE: {@code classNames} must be String constants.
     */
    public static void hasInstanceOf(Object x, String... classNames) {
    }

    /**
     * Asserts that pt(x) != pt(y).
     */
    public static void notEquals(Object x, Object y) {
    }

    /**
     * Asserts that pt(x) ^ pt(y) = {}
     */
    public static void disjoint(Object x, Object y) {
    }

    /**
     * Asserts that the call site right before this invocation
     * calls the specified methods.
     * NOTE: {@code methodSigs} must be String constants.
     */
    public static void calls(String... methodSigs) {
    }

    /**
     * Asserts that the call site right before this invocation
     * calls exactly the specified methods.
     * NOTE: {@code methodSigs} must be String constants.
     */
    public static void callsExact(String... methodSigs) {
    }

    /**
     * Asserts that the specified methods are reachable in the call graph.
     * NOTE: {@code methodSigs} must be String constants.
     */
    public static void reachable(String... methodSigs) {
    }
}
