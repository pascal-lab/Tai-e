class LongCallContext {
    public static void main(String[] args) {
        A a1 = new A();
        A a2 = new A();
        a1.foo(new B());
        a2.foo(new B());
        B result = a1.b;
    }
}

class A {
    B b;

    void foo(B b) {
        goo(b);
    }

    void goo(B b) {
        hoo(b);
    }

    void hoo(B b) {
        this.b = b;
    }
}

class B {
}