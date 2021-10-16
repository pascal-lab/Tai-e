public abstract class TestDumper {

    private static final double EPSILON = 1.0;
    private final int m;
    private final int n;
    private double[][] a;
    private String s;

    TestDumper() {
        m = 1;
        n = 0;
    }

    protected final int foo(int x, Object o) {
        int y = x + o.hashCode();
        if (y > 0) {
            y = 1;
        }
        return y;
    }

    abstract int abs(int x, double y);

    private abstract class A {}
    private interface I {}
}
