public class Exceptions {

    public static void main(String[] args) {
    }

    int array(int a[], int i) {
        int x = i + 2;
        int y = a[x];
        int z = x + y;
        return z;
    }

    int exception(int x) {
        try {
            if (x > 100) {
                foo(x);
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

    int throwEx(int x) throws Exception {
        int y = x * x;
        if (y > 0) {
            throw new Exception();
        }
        int z = x + y;
        return z;
    }

    int tryCatch(int x) throws Exception {
        int y = x * x;
        try {
            if (y > 0) {
                throw new RuntimeException();
            } else if (y < 0) {
                throwEx(0);
                throw new Exception();
            }
        } catch (RuntimeException e) {
            x = 100;
        }
        int z = x + y;
        return z;
    }

    int foo(int x) {
        return x;
    }
}
