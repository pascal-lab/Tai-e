import java.util.function.Function;

public class LambdaStaticMethod {

    public static void main(String[] args) {
        test();
    }

    static Object test() {
        Function<Object, Op> fun = (o) -> {
            if (o.hashCode() > 0) {
                return new GT();
            } else {
                return new LE();
            }
        };
        return fun.apply(new Object());
    }

    static abstract class Op {
    }

    static class GT extends Op {
    }

    static class LE extends Op {
    }
}
