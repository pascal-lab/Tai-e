/*
 * This testcase is taken from https://github.com/saffriha/ictac2014
 */

public class Inheritance {
    public static void main(String[] args) {
        B bb = new B(); bb.m(); bb.n(); bb.f();
        C cc = new C(); cc.m(); cc.n(); cc.f();
    }
}

class A {
    public Object a = new Object();
}

class B extends A {
    public Object m() { return this.a; }
    public Object n() { return ((A)this).a; }
    public Object f() { return (this == null) ? this.a : ((A) this).a; }
}

class C extends A {
    public Object a = new Object();

    public Object m() { return this.a; }
    public Object n() { return ((A)this).a; }
    public Object f() { return (this == null) ? this.a : ((A) this).a; }
}