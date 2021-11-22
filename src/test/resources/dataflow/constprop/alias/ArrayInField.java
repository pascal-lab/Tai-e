class ArrayInField {
    public static void main(String args[]) {
        foo();
        goo();
    }

    public static void foo() {
        A a1 = new A();
        a1.f = new int[2];
        a1.f[1] = 1;
        int r1 = a1.f[1];
    }

    public static void goo() {
        A a2 = new A();
        int array[] = new int[2];
        array[0] = 1;
        a2.f = array;
        int r2 = a2.f[0];
    }

}

class A {
    int[] f;
}
