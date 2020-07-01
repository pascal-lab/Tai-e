class Cast {
    public static void main(String[] args) {
        m();
    }

    static void m() {
        Object o = new A();
        o = new B();
        o = new C();
        A a = (A) o;
        B b = (B) o;
        C c = (C) o;
    }
}

class A {
}

class B {
}

class C extends B {
}
