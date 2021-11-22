class ArrayCorner {
    public static void main(String[] args) {
        int x = 1;
        if (x > 0) {
            x = 2;
        }

        corner1(x);
        corner2(x);
        corner3(x);
        corner4(x);
        corner5(x);
        corner6(x);
        corner7(x);
        corner8(x);
        corner9();
        corner10();
        corner11();
    }

    //corner1~corner8: different situation of NAC index store and load

    public static void corner1(int x) {
        int[] a = new int[2];
        a[x] = 1;
        a[x] = 2;
        int r1 = a[0];
    }

    public static void corner2(int x) {
        int[] b = new int[2];
        b[x] = 1;
        int r2 = b[0];
    }

    public static void corner3(int x) {
        int[] c = new int[2];
        c[0] = 1;
        c[x] = 2;
        int r3 = c[0];
    }

    public static void corner4(int x) {
        int[] d = new int[2];
        d[1] = 1;
        d[x] = 2;
        int r4 = d[0];
    }

    public static void corner5(int x) {
        int[] e = new int[2];
        int y = 1;
        if (x > 1) {
            y = 3;
        }
        e[x] = 1;
        e[y] = 2;
        int r5 = e[1];
    }

    public static void corner6(int x) {
        int[] f = new int[2];
        f[0] = 1;
        f[1] = 2;
        f[x] = 2;
        int r6 = f[x];
    }

    public static void corner7(int x) {
        int[] g = new int[3];
        g[x] = 1;
        g[2] = 6;
        g[x] = 6;
        int r7 = g[1];
    }

    public static void corner8(int x) {
        int[] h = new int[3];
        h[0] = 0;
        h[1] = 1;
        h[2] = 0;
        h[x] = 1;
        int r8_1 = h[0];
        int r8_2 = h[1];
    }

    public static void corner9() {//i is null pointer
        int[] i;
        i[0] = 1;
        int r9 = i[0];
    }

    //corner10~ : different sitution of UNDEF index store and load

    public static void corner10() {
        int[] j = new int[2];
        int y;
        j[y] = 0;
        j[1] = 1;
        int r10_1 = j[y];
        int r10_2 = j[1];
    }

    public static void corner11() {
        int[] k = new int[2];
        k[0] = 1;
        int y;
        int r11 = k[y];
    }


}