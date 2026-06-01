import sun.misc.Unsafe;

public class NonExistStaticField {
    public static void main(String[] args) {
        compareAndSwapObject();
    }

    static void compareAndSwapObject() {
        Unsafe unsafe = Unsafe.getUnsafe();
        A a = new A();
        String str = "str";
        unsafe.compareAndSwapObject(a, 4, null, str);
    }
}

class A {
    String str;

    static String static_str;
}
