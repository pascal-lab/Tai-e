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
        PTAAssert.equals(src1[0], dest1[0]);

        Object[] src2 = new Object[5];
        src2[0] = new String();
        String[] dest2 = new String[5];
        System.arraycopy(src2, 0, dest2, 0, 5);
        PTAAssert.equals(src2[0], dest2[0]);
    }

    static A staticA;

    static void doPrivileged() {
        A a = AccessController.doPrivileged(new PrivilegedAction<A>() {
            @Override
            public A run() {
                staticA = new A();
                return staticA;
            }
        });
        PTAAssert.notEmpty(a);
        PTAAssert.equals(a, staticA);
    }

    static void compareAndSwapObject() {
        Unsafe unsafe = Unsafe.getUnsafe();
        String expected = "expected";
        String[] arr = new String[]{ expected };
        String x = "x";
        unsafe.compareAndSwapObject(arr, 0, expected, x);
        PTAAssert.contains(arr[0], x);

        A a = new A();
        Object xo = new Object();
        unsafe.compareAndSwapObject(a, 0, null, x);
        PTAAssert.equals(a.str, x);
        unsafe.compareAndSwapObject(a, 4, null, xo);
        PTAAssert.equals(a.obj, xo);
    }
}

class A {

    String str;

    Object obj;
}
