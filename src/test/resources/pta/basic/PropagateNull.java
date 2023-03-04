class PropagateNull {

    public static void main(String[] args) {
        A a = null;
        if (args.length > 0) {
            a = new A();
        }
        String s = foo(a);
    }

    static String foo(A a) {
        a.f = null;
        Object nn = a.f;
        a.hashCode();

        Object o = null;
        String s = (String) o;
        return s;
    }
}

class A {

    Object f;
}
