import java.util.function.Function;

public class Capture {

    public static void main(String[] args) {
        Capture capture = new Capture();
        Function<Integer, Integer> addX = (n) -> capture.foo() + n;
        int y = addX.apply(100);
    }

    private int foo() {
        return 100;
    }
}
