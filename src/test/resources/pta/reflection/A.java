class A extends B {

    public static Object stat = new Object();

    public Object inst = new C();

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
    public Object baz(B b, String s) {
        return new Object();
    }

    public static void staticFoo(Object o) {}
}
