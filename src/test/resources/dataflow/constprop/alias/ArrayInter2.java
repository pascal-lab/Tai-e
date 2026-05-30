class ArrayInter2 {

    public static void main(String[] args) {
        int[] a = new int[1];
        set(a, 0, 777);
        int x;
        if (a[0] > 0) {
            x = get(a, 0);
        } else {
            x = get(a, 0);
        }
    }

    static void set(int[] arr, int i, int v) {
        arr[i] = v;
    }

    static int get(int[] arr, int i) {
        return arr[i];
    }
}
