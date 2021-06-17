class A extends B {

    public A(B b) {
    }

    A() {
    }

    private A(Object o) {
    }

    void foo() {
    }

    @Override
    public void foo(A a) {
    }

    private void foo(int i) {
    }

    void bar() {
    }

    @Override
    public void baz(B b) {
    }

    public static void staticFoo(Object o) {}
}
