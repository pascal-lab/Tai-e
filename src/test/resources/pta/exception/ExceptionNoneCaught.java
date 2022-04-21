public class ExceptionNoneCaught {

    public static void main(String[] args) {
        m();
        m1();
    }

    public static void m() throws ArithmeticException {
        m1();
        throw new ArithmeticException();
    }

    public static void m1() throws IllegalStateException, ArithmeticException {
        m();
        throw new IllegalStateException();
    }
}
