class ArrayInter {

    public static void main(String[] args) {
        int[] a = new int[3];
        set0(a, 123);
        set1(a, 666);
        set2(a, 987);
        int[] b = a;
        set2(b, 555);
        int x = a[0];
        int y = a[1];
        int z = a[2];
    }

    static void set0(int[] arr, int v) {
        arr[0] = v;
    }

    static void set1(int[] arr, int v) {
        arr[1] = v;
    }

    static void set2(int[] arr, int v) {
        arr[2] = v;
    }
}
