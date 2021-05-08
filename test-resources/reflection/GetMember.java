import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class GetMember {

    public static void main(String[] args) throws Exception {
        Class<A> klass = A.class;
        Constructor<A> ctor1 = klass.getConstructor(String.class);
        Constructor<A> ctor2 = klass.getDeclaredConstructor();
        Method foo1 = klass.getDeclaredMethod("foo", int.class);
        Method foo2 = klass.getMethod("foo", A.class);
        use(ctor1, ctor2, foo1, foo2);
    }

    static void use(Object... objs) {
    }

    static class B {
        public void foo(A a) {
        }

        public void foo(B b) {
        }
    }

    static class A extends B {

        public A(String s) {
        }

        A() {
        }

        private A(Object o) {
        }

        void foo() {
        }

        public void foo(A a) {
        }

        private void foo(int i) {
        }

        void bar() {
        }
    }
}
