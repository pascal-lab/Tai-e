import java.security.AccessController;
import java.security.PrivilegedAction;

class NativeModel {

    public static void main(String[] args) throws Exception {
        arraycopy();
        doPrivileged();
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
}

class A {
}
