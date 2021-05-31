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
        args.callOnThis("captureRecv");
    }

    void callOnThis(String s) {
        Runnable r = () -> consume(s);
        r.run();
    }

    void consume(String s) {}

    static void captureNoRecv() {
        String s = "captureNoRecv";
        Object o = new Object();
        A a = new A();
        Runnable r = () -> a.threeArgs(s, o, a);
        r.run();
    }

    static void actualRecv() {
        BiConsumer<A, String> bc = A::oneArg;
        bc.accept(new A(), "actualRecv");
    }

    static void actualNoRecv() {
        BiConsumer<A, Object> bc = (a, o) -> { a.hashCode(); o.hashCode(); };
        bc.accept(new A(), new Object());
    }

    static void captureWithActualRecv() {
        Args args = new Args();
        args.callOnThis2("captureWithActualRecv");
    }

    void callOnThis2(String s) {
        // capture receiver and s
        Consumer<String> c = (str) -> consume2(s, str);
        c.accept("callOnThis2");
    }

    void consume2(String s1, String s2) {}

    static void captureWithActualNoRecv() {
        String s = "captureWithActualNoRecv";
        A a = new A();
        Consumer<Object> c = o -> a.twoArgs(s, o);
        c.accept(new Object());
    }

    static class A {

        void noArg() {}

        void oneArg(String s) {}

        void twoArgs(String s, Object o) {}

        void threeArgs(String s, Object o, A a) {}
    }
}
