import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class LambdaConstructor {

    public static void main(String[] args) {
        Supplier<A> noArg = A::new;
        A a1 = noArg.get();
        String s1 = a1.name;
        use(s1);

        Function<String, A> oneArg = A::new;
        A a2 = oneArg.apply("Yeah");
        String s2 = a2.name;
        use(s2);

        BiFunction<String, String, A> twoArgs = A::new;
        A a3 = twoArgs.apply("N1", "N2");
        String s3 = a3.name;
        use(s3);
    }

    static void use(Object o) {}

    static class A {

        String name;

        A() {
            this.name = "Unknown";
        }

        A(String name) {
            this.name = name;
        }

        A(String name1, String name2) {
            if (hashCode() > 0) {
                name = name1;
            } else {
                name = name2;
            }
        }
    }
}
