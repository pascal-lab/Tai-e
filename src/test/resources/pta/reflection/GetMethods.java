import java.lang.reflect.Method;

public class GetMethods {

    public static void main(String[] args) throws Exception {
        invokeOneArg("foo");
        invokeOneArg("bar");
    }

    static void invokeOneArg(String name) throws Exception {
        Method[] methods = J.class.getMethods();
        for (Method m : methods) {
            if (m.getName().equals(name)) {
                J j = new J();
                m.invoke(j, j);
                PTAAssert.callsExact("<java.lang.reflect.Method: java.lang.Object invoke(java.lang.Object,java.lang.Object[])>",
                        "<I: void bar(I)>",
                        "<J: void foo(J)>",
                        "<java.lang.Object: boolean equals(java.lang.Object)>");
            }
        }
    }
}

class I {

    public void bar(I i) {
        System.out.println("I.bar(I)");
    }

    void bar(J j) {
        System.out.println("I.bar(J)");
    }
}

class J extends I {

    public void foo(J j) {
        System.out.println("J.foo(J)");
    }

    void foo(String s) {
        System.out.println("J.foo(String)");
    }
}
