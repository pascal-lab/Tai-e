public class Exceptions {

    private static int f;

    public static void main(String[] args) {
    }

    int noException(int a[], int i) {
        int x = i + 2;
        int y = x * i;
        int z = x + y;
        return z;
    }

    void implicitCaught(int i) {
        int x = i + i;
        try {
            int[] a = new int[x];
            x = a[i];
            f = x;
            use(x);
        } catch (Exception e) {
            x = i;
        } catch (Throwable t) {
            x = x * x;
        }
    }

    void implicitUncaught(int i) {
        int x = i + i;
        int[] a = new int[x];
        x = a[i];
        try {
            f = x;
            use(x);
        } catch (Exception e) {
            x = i;
        }
    }

    void throwCaught(int i) {
        try {
            int x = i * i;
            int[] a = new int[x];
            if (a.length > 0) {
                throw new IndexOutOfBoundsException();
            } else if (a.length > 10) {
                throw new IllegalStateException();
            } else {
                throw new ArithmeticException();
            }
        } catch (IllegalStateException e) {
        } catch (ArithmeticException e) {
        } catch (Exception e) {
        }
    }

    void throwUncaught(int i)
            throws ArithmeticException, IllegalStateException {
        if (i > 0) {
            use(i);
            throw new ArithmeticException();
        }
        try {
            if (i < 0) {
                throw new ArithmeticException();
            } else {
                throw new IllegalStateException();
            }
        } catch (ArithmeticException e) {
        }
    }

    int nestedThrowCaught(int x) {
        try {
            if (x > 100) {
                use(x);
                x = x * 10;
                try {
                    if (x == 0) {
                        throw new RuntimeException();
                    }
                } catch (RuntimeException re) {
                }
                throw new Exception();
            } else {
                use(10);
            }
        } catch (RuntimeException e) {
        } catch (Exception e) {
        }
        return x;
    }

    void declaredCaught(int i) {
        try {
            throwUncaught(i);
        } catch (RuntimeException e) {
        }
    }

    void declaredUncaught(int i) {
        try {
            throwUncaught(i);
        } catch (ArithmeticException e) {
        }
    }

    void duplicateException(ArithmeticException e) throws Exception {
        throwsAE();
        throw e;
    }

    void throwsAE() throws ArithmeticException {
        throw new ArithmeticException();
    }

    int nestedCatch(int i) {
        try {
            toString();
        } catch (NullPointerException npe) {
            try {
                int x = i / i;
            } catch (ArithmeticException ae) {
                return i;
            }
        }
        return 0;
    }

    static Object use(Object x) {
        return x;
    }
}
