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

public class ExceptionTreeAndRecursion {

    public static void main(String[] args) {
        try {
            m();
            m1();
        } catch (ArithmeticException e1) {
        } catch (IllegalStateException e2) {
        }
    }

    public static void m() throws ArithmeticException {
        throw new ArithmeticException();
    }

    public static void m1() throws IllegalStateException, ArithmeticException {
        try {
            m();
            m1();
            throw new IllegalStateException();
        } catch (ArithmeticException e1) {
        }
    }
}
