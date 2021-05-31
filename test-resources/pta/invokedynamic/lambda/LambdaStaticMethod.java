import java.util.function.Function;

public class LambdaStaticMethod {

    public static void main(String[] args) {
        test();
    }

    static Object test() {
        Function<Object, String> fun = (o) -> {
            if (o.hashCode() > 0) {
                return "> 0";
            } else {
                return "<= 0";
            }
        };
        return fun.apply(new Object());
    }
}
