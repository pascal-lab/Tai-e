class InstanceField {

    public static void main(String[] args) {
        A a = new A();
        a.longAP();
        a.cycle();
        a.callField();
    }
}

class A {
    B b;

    void longAP() {
        A a = new A();
        a.b = new B();
        a.b.c = new C();
        a.b.c.d = new D();
        D x = a.b.c.d;
    }

    void cycle() {
        A a = new A();
        B b = new B();
        b.a = a;
        a.b = b;
        A x = b.a.b.a;
    }

    void callField() {
        A a = new A();
        B b = new B();
        a.b = b;
        C c = a.b.foo();
    }
}

class B {
    A a;
    C c;

    C foo() {
        C x = new C();
        return x;
    }
}

class C {
    D d;
}

class D {
}
