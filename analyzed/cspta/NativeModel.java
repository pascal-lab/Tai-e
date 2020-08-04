class NativeModel {

    public static void main(String[] args) {
        objectClone();
        arraycopy();
    }

    static void objectClone() {
        A a = new A();
        Object o = a.callClone();
    }

    static void arraycopy() {
        Object[] src = new Object[5];
        src[0] = new Object();
        Object[] dest = new Object[5];
        System.arraycopy(src, 0, dest, 0, 5);
        Object o = dest[0];
    }
}

class A {
    Object callClone() {
        try {
            return clone();
        } catch (CloneNotSupportedException) {
            return null;
        }
    }
}
