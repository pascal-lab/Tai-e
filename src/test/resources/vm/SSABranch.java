public class SSABranch {

    public static void main(String[] args) {
        boolean ok = true;

        ok &= choose(1) == 10;
        ok &= choose(2) == 20;
        ok &= choose(3) == 30;

        if (ok) {
            System.out.println("OK");
        } else {
            System.out.println("FAIL");
        }
    }

    private static int choose(int x) {
        int y;
        if (x == 1) {
            y = 10;
        } else if (x == 2) {
            y = 20;
        } else {
            y = 30;
        }
        return y + 1 - 1;
    }
}
