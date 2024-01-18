import java.lang.reflect.Method;

public class ArgsRefine {

    public static void main(String[] args) throws Exception {
        invokePrint(new Class[]{Object.class, Object.class});
    }

    static void invokePrint(Class<?>[] paramTypes) throws Exception {
        Class bClass = Class.forName("B");
        Method print = bClass.getMethod("print", paramTypes);
        B b = new B();
        print.invoke(b, new Object[]{"hello", "hello"});
        PTAAssert.callsExact("<java.lang.reflect.Method: java.lang.Object invoke(java.lang.Object,java.lang.Object[])>",
                "<B: void print(java.lang.Object,java.lang.Object)>");
        print.invoke(b, "hello", "hello");
        PTAAssert.callsExact("<java.lang.reflect.Method: java.lang.Object invoke(java.lang.Object,java.lang.Object[])>",
                "<B: void print(java.lang.Object,java.lang.Object)>");
        Method printNoArg = bClass.getMethod("print");
        print.invoke(b);
        PTAAssert.callsExact("<java.lang.reflect.Method: java.lang.Object invoke(java.lang.Object,java.lang.Object[])>",
                "<B: void print()>");
    }
}

class B {

    public void print() {
        System.out.println("B.print()");
    }

    public void print(Object o) {
        System.out.println("B.print(Object)");
    }

    public void print(Object o1, Object o2) {
        System.out.println("B.print(Object,Object)");
    }

    public void print(Object o, B b) {
        System.out.println("B.print(Object,B)");
    }

    public void print(Object o1, Object o2, Object o3) {
        System.out.println("B.print(Object,Object,Object)");
    }
}
