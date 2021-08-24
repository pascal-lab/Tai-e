import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Test various cases of argument passing, including captured
 * and actual arguments.
 */
public class Args {

    public static void main(String[] args) {
        captureRecv();
        captureNoRecv();
        actualRecv();
        actualNoRecv();
        captureWithActualRecv();
        captureWithActualNoRecv();
    }

    static void captureRecv() {
        // method reference
        A a = new A();
        Runnable r = a::noArg;
        r.run();

        // lambda expression
        Args args = new Args();
        args.callOnThis(new B());
    }

    void callOnThis(B b) {
        Runnable r = () -> consume(b);
        r.run();
    }

    void consume(B b) {
    }

    static void captureNoRecv() {
        B b = new B();
        Object o = new Object();
        A a = new A();
        Runnable r = () -> a.threeArgs(b, o, a);
        r.run();
    }

    static void actualRecv() {
        BiConsumer<A, B> bc = A::oneArg;
        bc.accept(new A(), new B());
    }

    static void actualNoRecv() {
        BiConsumer<A, Object> bc = (a, o) -> {
            a.hashCode();
            o.hashCode();
        };
        bc.accept(new A(), new Object());
    }

    static void captureWithActualRecv() {
        Args args = new Args();
        args.callOnThis2(new B());
    }

    void callOnThis2(B b) {
        // capture receiver and b
        Consumer<B> c = (bb) -> consume2(b, bb);
        c.accept(new B());
    }

    void consume2(B b1, B b2) {
    }

    static void captureWithActualNoRecv() {
        B b = new B();
        A a = new A();
        Consumer<Object> c = o -> a.twoArgs(b, o);
        c.accept(new Object());
    }

    static class A {

        void noArg() {
        }

        void oneArg(B b) {
        }

        void twoArgs(B b, Object o) {
        }

        void threeArgs(B b, Object o, A a) {
        }
    }
}
