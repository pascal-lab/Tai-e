import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;

public class ExceptionPlugin{
    public static void main(String[] args) {
        try {
            m();
            m1();
        } catch (ArithmeticException e1) {
        } catch (IllegalStateException e2) {
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