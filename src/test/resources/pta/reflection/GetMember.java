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
        PTAAssert.sizeEquals(1, ctor1);
        Constructor<U> ctor2 = klass.getDeclaredConstructor();
        PTAAssert.sizeEquals(3, ctor2);
    }

    static void testMethod() throws Exception {
        Class<U> klass = U.class;
        Method foo1 = klass.getDeclaredMethod("foo", int.class);
        PTAAssert.sizeEquals(3, foo1);
        Method foo2 = klass.getMethod("foo", U.class);
        PTAAssert.sizeEquals(2, foo2);
    }

    static void testGetClass() throws Exception {
        U u = new U();
        Class<? extends U> klass = u.getClass();
        Method foo1 = klass.getDeclaredMethod("foo", int.class);
        PTAAssert.sizeEquals(3, foo1);
        Method foo2 = klass.getMethod("foo", U.class);
        PTAAssert.sizeEquals(2, foo2);
    }
}
