class OneObject {
    public static void main(String[] args) {
        m();
    }

    static void m() {
        A a1 = new A();
        A a2 = new A();
        B b1 = new B();
        B b2 = new B();
        a1.set(b1);
        a2.set(b2);
        B x = a1.get(); // x -> ?
    }
}

class A {
    B f;

    void set(B b) {
        this.doSet(b);
    }

    void doSet(B p) {
        this.f = p;
    }

    B get() {
        return this.f;
    }
}

class B {
}
