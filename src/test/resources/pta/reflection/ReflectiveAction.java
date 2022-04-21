import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectiveAction {
    public static void main(String[] args) throws Exception {
        forname(null);
        cnew();
        ctornew();
        arraynew();
        invoke();
        get();
        set();
    }

    static void forname(W w) throws Exception {
        Class<?> uClass = Class.forName("U");
        Object a = uClass.newInstance();
        a.hashCode();

        Class<?> vClass = Class.forName("V", true, uClass.getClassLoader());
        Object b = vClass.newInstance();
        b.hashCode();

        Class<?> wClass1 = Class.forName("W");
        use(wClass1);

        Class<?> wClass2 = Class.forName("W", true, uClass.getClassLoader());
        use(wClass2);
    }

    static void cnew() throws Exception {
        Class<U> klass = U.class;
        U u = klass.newInstance();
        u.foo();
    }

    static void ctornew() throws Exception {
        Class<U> klass = U.class;
        Constructor<U> ctor = klass.getConstructor(V.class);
        U u = ctor.newInstance(new V());
        u.foo(null);
    }

    static void arraynew() throws Exception {
        Class<?> uClass = new U().getClass();
        U[] arr = (U[]) Array.newInstance(uClass, 10);
        arr[0] = new U();
    }

    static void invoke() throws Exception {
        // invoke static method
        Method staticFoo = U.class.getMethod("staticFoo", Object.class);
        staticFoo.invoke(null, new Object[]{null});

        // invoke instance method
        Method baz = V.class.getMethod("baz", V.class, String.class);
        Object o = baz.invoke(new U(), new V(), "arg");
        use(o);
    }

    static void get() throws Exception {
        // get static field
        Field stat = U.class.getField("stat");
        Object o1 = stat.get(null);
        use(o1);

        // get instance field
        Field inst = U.class.getField("inst");
        Object o2 = inst.get(new U());
        use(o2);
    }

    static void set() throws Exception {
        // set static field
        Field stat = U.class.getField("stat");
        stat.set(null, new Object());

        // set instance field
        Field inst = U.class.getField("inst");
        inst.set(new U(), new V());
    }

    static void use(Object o) {
    }
}
