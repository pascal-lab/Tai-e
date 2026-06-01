public class Lambda {

    public static void main(String[] args) {
        int base = 3;

        IntFunction addBase = x -> x + base;
        IntFunction twice = Lambda::twice;

        check(addBase.apply(4) == 7);
        check(twice.apply(5) == 10);
        check(apply(6, x -> x * x) == 36);

        System.out.println("OK");
    }

    private static int apply(int value, IntFunction function) {
        return function.apply(value);
    }

    private static int twice(int value) {
        return value * 2;
    }

    private interface IntFunction {

        int apply(int value);
    }

    private static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
