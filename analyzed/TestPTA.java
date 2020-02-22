public class TestPTA {

    public static void main(String[] args) {
        test();
    }

    static void test() {
        A a1 = new A();
        A a2 = new A();
        A a3;
        a3 = a2;
        a3 = a1;
        a1.f = new C();
        a3.f = new D();
        B b1 = a1.f;
        b1.foo();
        B b2 = a3.f;
        b2.foo();
    }
}

class A {
    B f;
}

interface B {
    void foo();
}

class C implements B {
    @Override
    public void foo() {}
}

class D implements B {
    @Override
    public void foo() {}
}
