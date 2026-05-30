public class Primitive {

    public static void main(String[] args) {
        int i = 13;
        int j = 5;

        check(i + j == 18);
        check(i - j == 8);
        check(i * j == 65);
        check(i / j == 2);
        check(i % j == 3);

        check((i & j) == 5);
        check((i | j) == 13);
        check((i ^ j) == 8);
        check((i << 2) == 52);
        check((i >> 1) == 6);
        check((-1 >>> 1) == 2147483647);

        long x = 10_000_000_000L;
        long y = 3L;
        check(x + y == 10_000_000_003L);
        check(x / y == 3_333_333_333L);
        check(x % y == 1L);

        float f = 7.5f;
        float g = 2.0f;
        check(f + g == 9.5f);
        check(f / g == 3.75f);

        double d = 10.0;
        double e = 4.0;
        check(d * e == 40.0);
        check(d / e == 2.5);

        System.out.println("OK");
    }

    private static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
