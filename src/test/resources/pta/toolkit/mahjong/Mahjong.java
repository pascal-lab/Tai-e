public class Mahjong {
    public static void main(String[] args) {
        A x = new A();
        A y = new A();
        A z = new A();
        x.f = new B();
        y.f = new C();
        z.f = new C();
        A a = z.f;
        a.foo();
        C c = (C) a;
    }
}

class A {
    A f;
    void foo() {
    }
}

class B extends A {
    void foo() {
    }
}

class C extends A {
    void foo() {
    }
}
