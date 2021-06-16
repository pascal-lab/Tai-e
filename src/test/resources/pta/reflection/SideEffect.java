public class SideEffect {
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

    static void ctornew() {
    }

    static void invoke() {
    }
}
