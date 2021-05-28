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
    }
}
