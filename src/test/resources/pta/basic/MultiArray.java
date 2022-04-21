class MultiArray {

    public static void main(String[] args) {
        A a = new A().f();
    }
}

class A {

    A f() {
        int[] iarr = new int[10];
        iarr[0] = 0;

        A[][][] arr = new A[1][1][1];
        arr[0][0][0] = new A();
        A a = arr[0][0][0];

        A[][][] arr2 = new A[10][10][];
        arr2[0][0] = new A[2];
        A[] aa = arr2[0][0];
        return a;
    }
}
