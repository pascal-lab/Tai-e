import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class LambdaConstructor {

    public static void main(String[] args) {
        Supplier<A> noArg = A::new;
        A a1 = noArg.get();
        B b1 = a1.b;
        use(b1);

        Function<B, A> oneArg = A::new;
        A a2 = oneArg.apply(b1());
        B b2 = a2.b;
        use(b2);

        BiFunction<B, B, A> twoArgs = A::new;
        A a3 = twoArgs.apply(b2(), b3());
        B b3 = a3.b;
        use(b3);
    }

    static B b1() {
        return new B();
    }

    static B b2() {
        return new B();
    }

    static B b3() {
        return new B();
    }

    static void use(Object o) {
    }

    static class A {

        B b;

        A() {
            this.b = new B();
        }

        A(B b) {
            this.b = b;
        }

        A(B b1, B b2) {
            if (hashCode() > 0) {
                b = b1;
            } else {
                b = b2;
            }
        }
    }
}
