package com.example;

import com.example1.*;

public class X extends XFather {

    public void foo(String message) {
        message = "str"
    }

    public void foo(int number) {
        if (number == 0) {
            number = 1
        }
    }

    public static void main(String[] args) {
        X x = new X();
        X1 x1 = new X1();
        com.example1.X xx = new com.example1.X();
        com.example1.X1 x11 = new com.example1.X1();
        XFather xFather = new XFather();
        Y y = new Y();

        x.foo(0);
        xFather.foo(1);
        y.fun(x);
    }
}
