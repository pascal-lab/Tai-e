import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

class NativeModelWithLambda {

    public static void main(String[] args) throws Exception {
        doPrivilegedWithMethodRef();
        doPrivilegedWithLambda();
    }

    static void doPrivilegedWithMethodRef() throws PrivilegedActionException {
        A a = ((PrivilegedAction<A>) A::new).run();
        A a1 = AccessController.doPrivileged((PrivilegedAction<A>) A::new);
        A a2 = AccessController.doPrivileged((PrivilegedExceptionAction<A>) A::new);
    }

    static void doPrivilegedWithLambda() throws PrivilegedActionException {
        A a = ((PrivilegedAction<A>) () -> new A()).run();
        A a1 = AccessController.doPrivileged((PrivilegedAction<A>) () -> new A());
        A a2 = AccessController.doPrivileged((PrivilegedExceptionAction<A>) () -> new A());
    }

    static class A {
    }
}


