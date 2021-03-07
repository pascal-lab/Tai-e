public class AllInOne {

    public static void main(String[] args) {
    }

    int arrayAccess(int[][] a) {
        return a[0][0];
    }

    void newArray() {
        int[][] a1 = new int[10][];
        int[][][] a2 = new int[5][][];
        int[][][] a3 = new int[6][66][];
        int c [] = { 2, 8 };

        boolean [][] my_array = {
                {true, false, true},
                {},
                {false, false, true}
        };
    }

    void assign(String s) {
        String s2 = s;
        int x = 10;
        int y = x;
    }

    void binary(int x, int y, double z) {
        int a = x + y;
        int b = 1 - x;
        double d = 2.0 / 3.3;
        long l = 100 >> x;
        long ll = 100 >>> x;
    }

    void unary(int a[], int x) {
        if (a.length != 1) {
            a[1] = 10;
        }
        a[0] = -x;
    }

    void instanceOf(Object o) {
        boolean b = o instanceof AllInOne;
    }

    void cast(Object o, int i) {
        AllInOne a = (AllInOne) o;
        long l = i; // implicit type conversion

    }

    void invoke(int x, I i) {
        // invokevirtual
        int a = foo(x);
        // invokeinterface
        Object r = i.goo();
        // invokespecial
        AllInOne o = new AllInOne();
        super.toString();
        hidden();
        // invokestatic
        bar(10, null);
    }

    int returnInt(int x) {
        if (x > 0) {
            return x;
        }
        return 0;
    }

    void returnVoid() {
        foo(100);
        return;
    }

    static void bar(int x, Object o) {}

    int foo(int x) {
        return 20;
    }

    void exception(int x) {
        try {
            throw new Exception();
        } catch (RuntimeException e) {
            foo(20);
        } catch (Exception e) {
            hidden();
        }
    }

    void monitor(Object o) {
        synchronized (o) {
            hidden();
        }
        synchronized (this) {
            foo(0);
        }
    }

    private void hidden() {}
}

interface I {
    Object goo();
}
