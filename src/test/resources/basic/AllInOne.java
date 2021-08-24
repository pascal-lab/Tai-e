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
        int c[] = {2, 8};

        boolean[][] my_array = {
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

    void binaryMixedType(byte b, short s, boolean bb, char c, int i,
                         float f, long l) {
        int[] a = new int[10];
        // arithmetic
        a[0] = b + i;
        // bitwise
        a[0] = b & i;
        // compare

        // condition
        if (b > s) {
            a[0] = b;
        }
        // shift
        a[0] = s >> s;
        l = l >> s;
        a[0] = (int) l;
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

    int ifStmt(int x) {
        int y = 10;
        int z;
        if (x > 0) {
            z = x + 2;
        } else {
            foo(0, 1, 2);
            z = 10 + x;
        }
        return z;
    }

    int gotoStmt(int x) {
        for (int i = 0; i < x; ++i) {
            int z = 1 + x;
            if (x < 100) {
                continue;
            }
            int a = z - 10;
            if (a == 100) {
                break;
            }
            int b = a - z;
        }
        return 0;
    }

    int switchStmt(int x) {
        switch (x) {
            case 1:
                int y = 5 + x;
                return y;
            case 10:
                foo(10);
                return 500;
            case 100:
                foo(x);
                return 1000;
            default:
                bar(1, null);
        }
        x = x * 2;
        switch (x) {
            case 1:
                int y = 5 + x;
                return y;
            case 2:
                foo(10);
                return 500;
            case 5:
                foo(x);
                return 1000;
            default:
                bar(1, null);
        }
        return 0;
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

    int exception(int x) {
        try {
            if (x > 100) {
                hidden();
                x = x * 10;
                try {
                    if (x == 0) {
                        throw new RuntimeException();
                    }
                } catch (RuntimeException re) {
                    x = x + 1;
                }
                throw new Exception();
            } else {
                foo(10);
            }
        } catch (RuntimeException e) {
            foo(20);
        } catch (Exception e) {
        }
        x = x * 100;
        return x;
    }

    void emptyExceptionHandler() {
        try {
            throw new Exception();
        } catch (Exception e) {
        }
        foo(0);
        try {
        } catch (Exception e) {
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

    static void bar(int x, Object o) {
    }

    int foo(int x) {
        return 20;
    }

    int foo(int x, int y, int z) {
        return 30;
    }

    private void hidden() {
    }

//    @MyAnno
//    void testAnno() {}
}

interface I {
    Object goo();
}

//@interface MyAnno {}
