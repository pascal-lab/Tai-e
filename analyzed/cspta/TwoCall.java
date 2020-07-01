class TwoCall {
    public static void main(String[] args) {
        m();
    }

    static void m() {
        A a = new A();
        B b = a.id(new B());
        B c = a.id(new C());
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
