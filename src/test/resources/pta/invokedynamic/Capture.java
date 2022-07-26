import java.util.function.Function;
import java.util.function.Supplier;

public class Capture {

    public static void main(String[] args) {
        Capture capture = new Capture();
        Function<Integer, Integer> addX = (n) -> capture.foo() + n;
        int y = addX.apply(100);
        Supplier<Integer> supplier = capture.bar(200);
        supplier.get();
    }

    int foo() {
        return 100;
    }

    Supplier<Integer> bar(int x) {
        Supplier<Integer> addFoo = () -> foo() + x;
        return addFoo;
    }
}
