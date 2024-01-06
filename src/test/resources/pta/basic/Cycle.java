class Cycle {
    public static void main(String[] args) {
        cycle();
    }

    public static void cycle() {
        B b1 = new B();
        B b2 = new B();
        A a1 = new A(b1);
        A a2 = new A(b2);
        b2 = a1.b;
        b1 = a2.b;
        PTAAssert.equals(b1, b2);
    }
}

class A {
    B b;

    A(B b) {
        this.b = b;
    }
}

class B {

}
