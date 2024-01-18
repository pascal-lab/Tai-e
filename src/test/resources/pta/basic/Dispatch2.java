class Dispatch2 {
    public static void main(String[] args) {
        C1 c1 = new C1();
        c1.foo();
        PTAAssert.calls("<Dispatch2$I3: void foo()>");
        c1.bar();
        PTAAssert.calls("<Dispatch2$C1: void bar()>");
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
        }
    }
}
