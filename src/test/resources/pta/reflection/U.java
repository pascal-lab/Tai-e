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

class U extends V {

    public static Object stat = new Object();

    public Object inst = new W();

    public U(V v) {
    }

    U() {
    }

    private U(Object o) {
    }

    void foo() {
    }

    @Override
    public void foo(U u) {
    }

    private void foo(int i) {
    }

    void bar() {
    }

    @Override
    public Object baz(V v, String s) {
        return new Object();
    }

    public static void staticFoo(Object o) {}
}
