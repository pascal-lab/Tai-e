class OneType {
    public static void main(String[] args) {
        new A().m();
        new B().m();
    }
}

class A {
    void m() {
        C c1 = new C();
        c1.set(new D());
        C c2 = new C();
        c2.set(new D());
        D x = c1.get();
    }
}

class B {
    void m() {
        C c3 = new C();
        c3.set(new D());
        D y = c3.get();
    }
}

class C {
    D f;

    void set(D p) {
        this.f = p;
    }

    D get() {
        return this.f;
    }
}

class D {
}
