import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class ReflectiveAction {
    public static void main(String[] args) throws Exception {
        cnew();
        ctornew();
        invoke();
    }

    static void cnew() throws Exception {
        Class<A> klass = A.class;
        A a = klass.newInstance();
        a.foo();
    }

    static void ctornew() throws Exception {
        Class<A> klass = A.class;
        Constructor<A> ctor = klass.getConstructor(B.class);
        A a = ctor.newInstance(new B());
        a.foo(null);
    }

    static void invoke() throws Exception {
        // invoke static method
        Method staticFoo = A.class.getMethod("staticFoo", Object.class);
        staticFoo.invoke(null);

        // invoke instance method
        Method baz = B.class.getMethod("baz", B.class);
        baz.invoke(new A(), new B());
    }
}
