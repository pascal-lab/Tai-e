class Assign {

    int assign(int a, int b, int c) {
        int d = a + b;

        if (d > 0) {
            b = d;
        } else {
            b = 0;
        }

        if (d > 0) {
            c = a;
        } else {
            c = 0;
        }

        use(b);
        use(c);

        return b;
    }

    public static void use(int n) {}
}
