public class LongCallChain {

    public static void main(String[] args) {
        foo();
    }

    static void foo() {
        bar1();
        bar2();
    }

    static void bar1() {
        baz1();
        baz2();
    }

    static void bar2() {
    }

    static void baz1() {
        A a = new A();
        a.m1();
    }

    static void baz2() {
    }
}

class A {
    void m1() {
        m2();
    }

    void m2() {
        m3();
    }

    void m3() {
        m4();
    }

    void m4() {
    }
}
