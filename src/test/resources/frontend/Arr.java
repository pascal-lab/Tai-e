class Arr {
    public void f() {
        int[] a = new int[3];
        a[0] = 1;
        a[2] = 10;
    }

    public void g() {
        int[][] a = new int[3][];
        a[0] = new int[] { 1, 2, 3 };
        return;
    }

    public void k() {
        int[][][] a = { { { 1,2,3 } } };
        a[0][0][0] = 10;
        return;
    }

    public void a() {
        A[] a = new A[10];
        a[0] = new A();
        return;
    }

}

class A {

}