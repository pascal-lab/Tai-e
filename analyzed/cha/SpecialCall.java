public class SpecialCall {

    public static void main(String[] args) {
        A a1 = new A();
        B b1 = new B();
        b1.callInteresting();
        B b2 = new C();
        A a2 = new A(b2);
    }
}

class A {

    A() {}

    A(B b) {}

    private void interesting() {
        System.out.println("A.interesting()");
    }

    void foo() {}

    void callInteresting() {
        System.out.println("this: " + this);
        interesting();
    }
}

class B extends A {
    void interesting() {
        System.out.println("B.interesting()");
    }
}

class C extends B {

    C() {
        super.foo();
    }
}
