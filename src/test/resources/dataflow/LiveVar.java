/**
 * Test case for live variable analysis.
 */
public class LiveVar {

    public static void main(String[] args) {
    }

    int assign(int a, int b, int c) {
        int d = a + b;
        b = d;
        c = a;
        return b;
    }

    int invoke(int a, int b, int c) {
        int x = a - b;
        int y = a - x;
        int z = x;
        invoke(0, 0, 0);
        return z;
    }

    int loopBranch(int m, int n, int k) {
        int a, i;
        for (i = m - 1; i < k; i++) {
            if (i >= n) {
                a = n;
            }
            a = a + i;
        }
        return a;
    }

    void branchLoop(int c, boolean d) {
        int x, y, z;
        x = 1;
        y = 2;
        if (c > 0) {
            do {
                x = y + 1;
                y = 2 * z;
                if (d) {
                    x = y + z;
                }
                z = 1;
            } while (c < 20);
        }
        z = x;
    }
}
