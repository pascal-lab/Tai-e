import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;

public class ExceptionCircle {
    public static void main(String[] args) {
        try {
            m();
            m1();
        } catch (ArithmeticException e1) {
        } catch (IllegalStateException e2) {
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