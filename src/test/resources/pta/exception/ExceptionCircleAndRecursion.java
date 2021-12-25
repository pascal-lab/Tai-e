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

public class ExceptionCircleAndRecursion {

    public static void main(String[] args) {
        try {
            m(2);
            m1(3);
        } catch (ArithmeticException e1) {
            e1.getMessage();
        } catch (IllegalStateException e2) {
            e2.getMessage();
        }
    }

    public static void m(int n) throws ArithmeticException {
        m1(5);
        if (n >= 0) {
            m(n - 1);
        }
        throw new ArithmeticException();
    }

    public static void m1(int k) throws IllegalStateException, ArithmeticException {
        m(6);
        if (k >= 2) {
            m1(k - 2);
        }
        throw new IllegalStateException();
    }
}
