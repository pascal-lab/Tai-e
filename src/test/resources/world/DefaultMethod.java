public class DefaultMethod {

    interface I {
        default void bar() {
        }
    }

    interface II extends I {
        default void foo() {
        }

        default void bar() {
        }
    }

    class A {
        public void foo() {
        }
    }

    class B extends A {
    }

    class C extends B implements II {
    }
}
