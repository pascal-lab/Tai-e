import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class GetMember {

    public static void main(String[] args) throws Exception {
        testConstructor();
        testMethod();
        testGetClass();
    }

    static void testConstructor() throws Exception {
        Class<U> klass = U.class;
        Constructor<U> ctor1 = klass.getConstructor(V.class);
        Constructor<U> ctor2 = klass.getDeclaredConstructor();
        use(ctor1, ctor2);
    }

    static void testMethod() throws Exception {
        Class<U> klass = U.class;
        Method foo1 = klass.getDeclaredMethod("foo", int.class);
        Method foo2 = klass.getMethod("foo", U.class);
        use(foo1, foo2);
    }

    static void testGetClass() throws Exception {
        U u = new U();
        Class<? extends U> klass = u.getClass();
        Method foo1 = klass.getDeclaredMethod("foo", int.class);
        Method foo2 = klass.getMethod("foo", U.class);
        use(foo1, foo2);
    }

    static void use(Object... objs) {
    }

    // We need to trigger the reference analysis for A, otherwise
    // A may not be resolved by frontend.
    void refA(U u) {
    }
}
