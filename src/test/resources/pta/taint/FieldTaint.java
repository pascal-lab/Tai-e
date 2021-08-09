class FieldTaint {

    public static void main(String[] args) {
        A a = new A();
        B b = new B();
        b.g = a;
        b.g.f = SourceSink.source();
        foo(b);
    }

    static void foo(B b) {
        SourceSink.sink(b.g.f); // taint
    }

    static class A {
        String f;
    }

    static class B {
        A g;
    }
}
