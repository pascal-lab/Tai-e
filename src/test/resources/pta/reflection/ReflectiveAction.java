import java.lang.reflect.Constructor;

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

    static void invoke() {
    }
}
