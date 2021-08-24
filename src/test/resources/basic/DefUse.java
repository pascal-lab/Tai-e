public class DefUse {

    int foo(int a, X x, X[] xs) {
        int b = a + a;
        int c;
        if (b > 0) {
            c = b;
        } else {
            c = a;
        }
        x.f = c;
        int d = x.f;
        xs[a] = x;
        use(a, b, c);
        X.sf = a;
        c = X.sf;
        return c;
    }

    void exception(int i) {
        try {
            if (i > 0) {
                throw new Exception();
            }
        } catch (Exception e) {
        }
    }

    void switchCase(int i) {
        switch (i) {
            case 1:
                return;
            case 10:
                use(i);
                break;
            case 100:
                use(i + 1);
        }
    }

    void array(int a, int b, int c) {
        int[] arr = new int[a];
        int[][][] aaarr = new int[a][b][c];
        arr[a] = 100;
        aaarr[a][b][c] = 100;
    }

    int exp(int a, long b, Object o) {
        b = (long) a;
        if (o instanceof X) {
            return a;
        }
        return 0;
    }

    void use(int i1, int i2, int i3) {
    }

    void use(int... is) {
    }

    static class X {
        static int sf;
        int f;
    }
}
