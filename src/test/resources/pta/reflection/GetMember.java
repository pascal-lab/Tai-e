import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class GetMember {

    public static void main(String[] args) throws Exception {
        Class<U> klass = U.class;
        Constructor<U> ctor1 = klass.getConstructor(V.class);
        Constructor<U> ctor2 = klass.getDeclaredConstructor();
        Method foo1 = klass.getDeclaredMethod("foo", int.class);
        Method foo2 = klass.getMethod("foo", U.class);
        use(ctor1, ctor2, foo1, foo2);
    }

    static void use(Object... objs) {
    }

    // We need to trigger the reference analysis for A, otherwise
    // A may not be resolved by frontend.
    void refA(U u) {
    }
}
