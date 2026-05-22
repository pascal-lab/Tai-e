public class SSANestedBranch {

    public static void main(String[] args) {
        boolean ok = true;

        ok &= compute(3, 4) == 15;
        ok &= compute(5, 2) == 7;
        ok &= compute(8, 3) == 15;

        if (ok) {
            System.out.println("OK");
        } else {
            System.out.println("FAIL");
        }
    }

    private static int compute(int a, int b) {
        int x;
        if (a < b) {
            x = a + b;
        } else {
            if (a - b > 3) {
                x = a + b - 1;
            } else {
                x = a - b;
            }
        }

        int y;
        if (x % 2 == 0) {
            y = x / 2;
        } else {
            y = x + 1;
        }

        return y + x;
    }
}
