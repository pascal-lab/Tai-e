public class CFG {

    void empty() {
    }

    int ifStmt(int x) {
        int a = x * x;
        int b = 0;
        if (a > 100) {
            b = a / x;
        } else if (a < 10) {
            b = a + x;
        } else {
            b = a - x;
        }
        return b;
    }

    int forLoop(int n) {
        int[] a = new int[n];
        for (int i = 0; i < n; ++i) {
            a[i] = i * i;
        }
        return a[0];
    }

    int whileLoop(int n) {
        int i = 0;
        int j = 0;
        while (i < n) {
            j = j + i;
            ++i;
        }
        return j;
    }

    int breakLoop(int n) {
        int x = n * n;
        while (x > n) {
            x = x - 1;
            if (x == 100) {
                x = 20;
                break;
            }
            x = x - 2;
        }
        x = x * 5;
        return x;
    }

    int switchCase(int n) {
        int x;
        switch (n) {
            case 1:
                x = 1;
                break;
            case 2:
                x = 2;
                break;
            case 3:
                x = 3;
                break;
            default:
                x = 100;
        }
        int y = x * x;
        int z;
        switch (y) {
            case 1:
                z = 1;
                break;
            case 100:
                z = 100;
                break;
            case 1000:
                z = 1000;
                break;
            default:
                z = 10000;
        }
        return z;
    }

    int multiReturn(int n) {
        if (n > 0) {
            return 1;
        } else if (n < 0) {
            return -1;
        } else {
            return 0;
        }
    }
}
