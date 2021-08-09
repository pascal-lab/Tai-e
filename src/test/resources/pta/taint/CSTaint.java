class CSTaint {

    public static void main(String[] args) {
        A a1 = new A();
        String s1 = new String();
        a1.set(s1);
        SourceSink.sink(a1.get());

        A a2 = new A();
        String s2 = SourceSink.source();
        a2.set(s2);
        SourceSink.sink(a2.get()); // taint
    }

    static class A {
        String f;

        String get() {
            return f;
        }

        void set(String s) {
            this.f = s;
        }
    }
}
