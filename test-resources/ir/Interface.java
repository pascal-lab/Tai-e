import java.util.UUID;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class Interface {

    public static void main(String[] args) {

        //  1. Predicate: T -> boolean
        Predicate<String> pre = "admin"::equals;
        System.out.println(pre.test("manager"));

        // 2. Consumer: T -> ()
        Consumer<String> con = (String message) -> {
            System.out.println("hello: " + message);
            System.out.println("finished");
        };
        con.accept("guys");

        // 3. Function: T -> R
        Function<String, Integer> fun = (String gender) -> {
            return "male".equals(gender) ? 1 : 0;
        };
        System.out.println(fun.apply("male"));

        // 4. Supplier: () -> T (by get())
        Supplier<String> sup = () -> UUID.randomUUID().toString();
        System.out.println(sup.get());

        // 5. UnaryOperator: T -> T
        UnaryOperator<String> uo = (String img) -> {
            img += "[100x200]";
            return img;
        };
        System.out.println(uo.apply("origin graph"));

        // 6. BinaryOperator: (T, T) -> T
        BinaryOperator<Integer> bo = (Integer i1, Integer i2) -> i1 > i2 ? i1 : i2;
        System.out.println(bo.apply(12, 13));
    }
}
