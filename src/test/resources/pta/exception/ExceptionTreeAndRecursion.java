public class ExceptionTreeAndRecursion {

    public static void main(String[] args) {
        try {
            m();
            m1();
        } catch (ArithmeticException e1) {
            e1.getMessage();
        } catch (IllegalStateException e2) {
            e2.getMessage();
        }
    }

    public static void m() throws ArithmeticException {
        throw new ArithmeticException();
    }

    public static void m1() throws IllegalStateException, ArithmeticException {
        try {
            m();
            m1();
            throw new IllegalStateException();
        } catch (ArithmeticException e1) {
        }
    }
}
