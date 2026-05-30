class TwoCall {
    public static void main(String[] args) {
        m();
    }

    static void m() {
        A a = new A();
        B b = a.id(new B()); b.hashCode();
        B c = a.id(new C()); c.hashCode();
    }
}

class A {
    B id(B b) {
        return _id(b);
    }

    B _id(B p) {
        return p;
    }
}

class B {
}

class C extends B {
}
