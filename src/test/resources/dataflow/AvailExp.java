/**
 * Test case for available expression analysis.
 */
class AvailExp {

    int foo(int a, int b, int c) {
        int p = a, q = b, r;
        long l;
        if (a > 0) {
            l = (long) a;
            p = a + b;
            q = b - c;
            r = q;
        } else {
            l = (long) a;
            q = a + b;
            r = b - c;
            c = r;
        }
        r = p;
        use(a, b, c);
        use((int) l, r, q);
        return r + q;
    }

    void use(int i1, int i2, int i3) {
    }
}
