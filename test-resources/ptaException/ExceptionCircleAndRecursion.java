import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;

public class ExceptionCircleAndRecursion {
    public static void main(String[] args) {
        try {
            m(2);
            m1(3);
        } catch (ArithmeticException e1) {
        } catch (IllegalStateException e2) {
        }
    }

    public static void m(int n) throws ArithmeticException {
        m1(5);
        if(n>=0){
         m(n-1);
        }
        throw new ArithmeticException();
    }

    public static void m1(int k) throws IllegalStateException, ArithmeticException {
        m(6);
        if(k>=2){
            m1(k-2);
        }
        throw new IllegalStateException();
    }
}