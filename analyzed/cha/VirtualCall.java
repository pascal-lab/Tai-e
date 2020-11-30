public class VirtualCall {

    public static void main(String[] args) {
        B b = new B();
        b.foo();
    }
}

class A {
    void foo() {
    }
}

class B extends A {
}

class C extends B {
    void foo() {
    }
}

class D extends B {
    void foo() {
    }
}

class E extends A {
    void foo() {
    }
}
