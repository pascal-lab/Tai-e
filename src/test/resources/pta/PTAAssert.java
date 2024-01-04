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

public final class PTAAssert {

    /**
     * Asserts that pt(x) ≠ Ø.
     */
    public static void notEmpty(Object x) {
    }

    /**
     * Asserts that |pt(x)| = size.
     * NOTE: {@code size} must be an integer constant.
     */
    public static void sizeEquals(Object x, int size) {
    }

    /**
     * Asserts that pt(x) contains object of specified class.
     * NOTE: {@code className} must be a String constant.
     */
    public static void hasInstanceOf(Object x, String className) {
    }

    /**
     * Asserts that pt(x) = pt(y).
     */
    public static void equals(Object x, Object y) {
    }

    /**
     * Asserts that pt(x) ≠ pt(y).
     */
    public static void notEquals(Object x, Object y) {
    }

    /**
     * Asserts that pt(x) ⊇ pt(y).
     */
    public static void contains(Object x, Object y) {
    }

    /**
     * Asserts that pt(x) ∩ pt(y) = Ø
     */
    public static void disjoint(Object x, Object y) {
    }

    /**
     * Asserts that the call site right before this invocation
     * calls the specified method.
     * NOTE: {@code methodSig} must be a String constant.
     */
    public static void calls(String methodSig) {
    }

    /**
     * Asserts that the specified method is reachable in the call graph.
     * NOTE: {@code methodSig} must be a String constant.
     */
    public static void isReachable(String methodSig) {
    }
}
