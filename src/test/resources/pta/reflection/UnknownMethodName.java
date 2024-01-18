import java.lang.reflect.Method;

public class UnknownMethodName {

    public static void main(String[] args) throws Exception {
        invokeNonArg(unknown("nonArg"));
        invokeOneArg(unknown("oneArg"), new Class[]{D.class});
        invokeOneArg(unknown("oneArg"), new Class[]{Object.class});
        invokeOneArg(unknown("oneArg"), new Class[]{eClass()});
        invokeOneArgRet(unknown("oneArgRet"), new Class[]{D.class});
    }

    static void invokeNonArg(String name) throws Exception {
        Method nonArg = eClass().getMethod(name);
        nonArg.invoke(null);
        PTAAssert.callsExact("<java.lang.reflect.Method: java.lang.Object invoke(java.lang.Object,java.lang.Object[])>",
                "<E: void nonArg()>");
    }

    static void invokeOneArg(String name, Class<?>[] paramTypes) throws Exception {
        Method oneArg = eClass().getMethod(name, paramTypes);
        E e = new E();
        oneArg.invoke(e, e);
        PTAAssert.callsExact("<java.lang.reflect.Method: java.lang.Object invoke(java.lang.Object,java.lang.Object[])>",
                "<E: void oneArg(D)>", "<E: void oneArg(java.lang.Object)>", "<E: D oneArgRet(D)>",
                "<D: void oneArg(E)>",
                "<java.lang.Object: boolean equals(java.lang.Object)>");
    }

    static void invokeOneArgRet(String name, Class<?>[] paramTypes) throws Exception {
        Method oneArg = eClass().getMethod(name, paramTypes);
        E e = new E();
        D d = (D) oneArg.invoke(e, e); // <E: D oneArgRet(D)>
        PTAAssert.callsExact("<java.lang.reflect.Method: java.lang.Object invoke(java.lang.Object,java.lang.Object[])>",
                "<E: D oneArgRet(D)>");
        PTAAssert.contains(d, e);
    }

    static Class<?> eClass() throws Exception {
        return Class.forName("E");
    }

    static String unknown(String s) {
        return new String(s);
    }
}

class D {

    public void oneArg(D d) {
        System.out.println("D.oneArg(D)");
    }

    public void oneArg(E e) {
        System.out.println("D.oneArg(E)");
    }
}

class E extends D {

    static void packagePrivate() {
        System.out.println("E.packagePrivate()");
    }

    public static void nonArg() {
        System.out.println("E.nonArg()");
    }

    public void oneArg(Object o) {
        System.out.println("E.oneArg(Object)");
    }

    @Override
    public void oneArg(D d) {
        System.out.println("E.oneArg(D)");
    }

    public void oneArg(String s) {
        System.out.println("E.oneArg(String)");
    }

    public D oneArgRet(D d) {
        System.out.println("E.oneArgRet(D)");
        return d;
    }
}
