class Dispatch2 {
    public static void main(String[] args) {
        C1 c1 = new C1();
        c1.bar();
    }

    static interface I1 {
        public void foo();
    }

    static interface I2 extends I1 {
    }

    static interface I3 extends I2 {
        @Override
        default void foo() {
        }
    }

    static interface I4 extends I2, I3 {
    }

    static class C1 implements I4 {
        public void bar() {
            I4.super.foo();
        }
    }
}
