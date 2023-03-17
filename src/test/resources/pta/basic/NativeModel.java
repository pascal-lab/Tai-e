import java.security.AccessController;
import java.security.PrivilegedAction;
import sun.misc.Unsafe;

class NativeModel {

    public static void main(String[] args) throws Exception {
        arraycopy();
        doPrivileged();
        compareAndSwapObject();
    }

    static void arraycopy() {
        Object[] src1 = new Object[5];
        src1[0] = new Object();
        Object[] dest1 = new Object[5];
        System.arraycopy(src1, 0, dest1, 0, 5);
        Object o = dest1[0];

        Object[] src2 = new Object[5];
        src2[0] = new String();
        String[] dest2 = new String[5];
        System.arraycopy(src2, 0, dest2, 0, 5);
        String s = (String) dest2[0];
    }

    static void doPrivileged() {
        A a = AccessController.doPrivileged(new PrivilegedAction<A>() {
            @Override
            public A run() {
                return new A();
            }
        });
    }

    static void compareAndSwapObject() {
        Unsafe unsafe = Unsafe.getUnsafe();
        String expected = "expected";
        String[] arr = new String[]{ expected };
        unsafe.compareAndSwapObject(arr, 0, expected, "x");
        String x = arr[0];
        System.out.println(x);

        A a = new A();
        Object xo = new Object();
        unsafe.compareAndSwapObject(a, 0, null, "x");
        unsafe.compareAndSwapObject(a, 4, null, xo);
        String astr = a.str;
        Object aobj = a.obj;
    }
}

class A {

    String str;

    Object obj;
}
