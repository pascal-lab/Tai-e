public class TestPTA {

    public static void main(String[] args) {
        test();
        test1obj();
        test1call();
    }

    static void test() {
        A a1 = new A();
        A a2 = new A();
        A a3;
        a3 = a2;
        a3 = a1;
        a1.b = new C();
        a3.b = new D();
        B b1 = a1.b;
        b1.foo();
        B b2 = a3.b;
        b2.foo();
    }

    static void test1obj() {
        A a1 = new A();
        A a2 = new A();
        a1.setF(new C());
        B x1 = a1.getB();
        a2.setF(new D());
        B x2 = a2.getB();
    }

    static void test1call() {
        A a = new A();
        B o1 = a.identity(new C());
        B o2 = a.identity(new D());
    }
}

class A {
    B b;

    void setF(B b) {
        this.b = b;
    }

    B getB() {
        return b;
    }

    B identity(B b) {
        return b;
    }
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
