public class SpecialCall {

    public static void main(String[] args) {
        A a1 = new A();
        B b = new C();
        A a2 = new A(b);
    }
}

class A {

    A() {
    }

    A(B b) {
    }

    void foo() {
    }
}

class B extends A {
}

class C extends B {

    C() {
        super.foo();
    }

    void foo() {
    }
}
