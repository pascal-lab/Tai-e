import java.util.function.Function;

public class Capture {

    public static void main(String[] args) {
        int x = args.length;
        Function<Integer, Integer> addX = (n) -> x + n;
        int y = addX.apply(100);
    }
}
