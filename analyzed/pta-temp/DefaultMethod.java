class DefaultMethod {

    public static void main(String[] args) {
        I i = new C();
        i.foo();
        C c = new C();
        c.foo();
    }

    interface I {
        default void foo() {
        }

        void bar();
    }

    static class C implements I {
        @Override
        public void bar() {
        }
    }
}
