public class SSALoop {

    public static void main(String[] args) {
        boolean ok = true;

        ok &= sum(5) == 15;
        ok &= product(4) == 24;

        if (ok) {
            System.out.println("OK");
        } else {
            System.out.println("FAIL");
        }
    }

    private static int sum(int n) {
        int result = 0;
        int i = 1;
        while (i <= n) {
            result += i;
            ++i;
        }
        return result;
    }

    private static int product(int n) {
        int result = 1;
        for (int i = 1; i <= n; ++i) {
            result *= i;
        }
        return result;
    }
}
