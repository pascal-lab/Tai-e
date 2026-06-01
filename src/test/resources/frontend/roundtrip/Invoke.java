public class Invoke {

    public static void main(String[] args) {
        Derived derived = new Derived(4);
        Base base = derived;

        check(staticAdd(2, 3) == 5);
        check(derived.privateCall(6) == 18);
        check(base.value() == 8);
        check(derived.superValue() == 5);
        check(derived.overload(3) == 4);
        check(derived.overload(3, 4) == 7);

        System.out.println("OK");
    }

    private static int staticAdd(int a, int b) {
        return a + b;
    }

    private static class Base {

        final int seed;

        Base(int seed) {
            this.seed = seed;
        }

        int value() {
            return seed + 1;
        }
    }

    private static class Derived extends Base {

        Derived(int seed) {
            super(seed);
        }

        @Override
        int value() {
            return seed * 2;
        }

        int superValue() {
            return super.value();
        }

        int privateCall(int x) {
            return triple(x);
        }

        int overload(int x) {
            return x + 1;
        }

        int overload(int x, int y) {
            return x + y;
        }

        private int triple(int x) {
            return x * 3;
        }
    }

    private static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
