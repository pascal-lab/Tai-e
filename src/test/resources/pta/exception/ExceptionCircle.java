public class ExceptionCircle {

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
        m1();
        throw new ArithmeticException();
    }

    public static void m1() throws IllegalStateException, ArithmeticException {
        m();
        throw new IllegalStateException();
    }
}
