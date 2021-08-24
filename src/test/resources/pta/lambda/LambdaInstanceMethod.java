import java.util.function.Supplier;

public class LambdaInstanceMethod {

    public static void main(String[] args) {
        interfaze();
        virtual();
        special();
    }

    static void interfaze() {
        I i = new C();
        Runnable r = i::foo;
        r.run();
    }

    static void virtual() {
        A a = new B();
        Runnable r = a::bar;
        r.run();
    }

    static void special() {
        C c = new C();
        c.addOne(100);
    }

    interface I {
        void foo();
    }

    static class C implements I {
        public void foo() {
        }

        C() {
        }

        C(int n) {
        }

        C addOne(int x) {
            Supplier<C> addOne = () -> new C(x + one());
            return addOne.get();
        }

        int one() {
            return 1;
        }
    }

    static abstract class A {
        void bar() {
        }
    }

    static class B extends A {
        void bar() {
        }
    }
}
