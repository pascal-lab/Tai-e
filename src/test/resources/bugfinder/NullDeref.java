class NullDeref {

    int y;
    Object field;

    void nullObject() {
        NullDeref n = null;
//        assert n != null;
        n.f(1);
        f(n.y);
    }

    public static void main(String[] args) {
    }

    public NullDeref() {
    }

    void f(int x) {
    }

    Object getAObject() {
        return new Object();
    }

    void allStmts(Object o) {
        Object n = new Object();
        n = field;
        n = null;
        n = "nonNull";
        field = n;

        Object n2 = n;
        n2 = this;

        Object[] array = new Object[10];
        n2 = array[0];
        n2 = getAObject();

        NullDeref nd = new NullDeref();
        Object n3 = (Object) nd;
        n3 = (Object) null;
    }

    void nullArray() {
        NullDeref[] a = null;
        int b = a.length;
        a[b].f(b);
    }

    public boolean equals(Object o) {
        return false;
    }

    void throwNull() throws Throwable {
        Throwable a = null;
        throw a;
    }

    void nullComparison() {
        Object a = null;
        Object[] array = null;
        if (a == null) { // report warning
            nullArray();
        }
        if (array != null) {// report warning
            nullArray();
        }
        if (null != a) {// report warning
            nullArray();
        }
        Object b = new Object();
        if (a != b) {// report warning
            throwNull();
        }
        if (a instanceof NullDeref) {
            throwNull();
        }
    }
}
