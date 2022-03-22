class Array {

    public static void main(String[] args) {
        A[] arr = new A[10];
        arr[0] = new A();
        arr[1] = new A();
        A a = arr[0];
        arr.hashCode();
        B[] barr = new B[10];
        arrayStore(barr, new A());
        Object o = barr[0];
    }

    private static void arrayStore(Object[] a, Object o) {
        a[0] = o;
    }
}

class A {
}

class B {
}
