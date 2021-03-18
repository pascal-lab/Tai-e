class ClassObj {

    static Class klass;

    public static void main(String[] args) {
        Class c1 = A.class;
        c1.hashCode();
        klass = A.class;
        Class c2 = klass;
        c2.hashCode();
    }

    static class A {

        static {
            Object o = new Object();
        }
    }
}
