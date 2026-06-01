public class Exception {

    public static void main(String[] args) {
        check(catchException(0) == 10);
        check(catchException(2) == 5);
        check(throwAndCatch() == 7);

        System.out.println("OK");
    }

    private static int catchException(int x) {
        try {
            if (x == 0) {
                throw new IllegalArgumentException("zero");
            }
            return 10 / x;
        } catch (IllegalArgumentException e) {
            return 10;
        }
    }

    private static int throwAndCatch() {
        try {
            throw new RuntimeException("test");
        } catch (RuntimeException e) {
            return 7;
        }
    }

    private static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
