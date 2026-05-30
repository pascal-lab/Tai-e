public class ControlFlow {

    public static void main(String[] args) {
        check(abs(-7) == 7);
        check(abs(5) == 5);
        check(sumOddUntil(10) == 25);
        check(branch(3, 4) == 7);
        check(branch(5, 2) == 3);
        check(denseSwitch(2) == 30);
        check(denseSwitch(9) == -1);
        check(sparseSwitch(-100) == 1);
        check(sparseSwitch(1000) == 3);
        check(sparseSwitch(7) == -1);
        check(fallThrough(1) == 6);
        check(fallThrough(2) == 5);
        check(fallThrough(4) == -1);

        System.out.println("OK");
    }

    private static int abs(int x) {
        if (x < 0) {
            return -x;
        }
        return x;
    }

    private static int sumOddUntil(int limit) {
        int sum = 0;
        int i = 0;
        while (i <= limit) {
            ++i;
            if (i % 2 == 0) {
                continue;
            }
            if (i > limit) {
                break;
            }
            sum += i;
        }
        return sum;
    }

    private static int branch(int a, int b) {
        if (a > b) {
            return a - b;
        }
        return a + b;
    }

    private static int denseSwitch(int x) {
        return switch (x) {
            case 0 -> 10;
            case 1 -> 20;
            case 2 -> 30;
            case 3 -> 40;
            default -> -1;
        };
    }

    private static int sparseSwitch(int x) {
        return switch (x) {
            case -100 -> 1;
            case 0 -> 2;
            case 1000 -> 3;
            default -> -1;
        };
    }

    private static int fallThrough(int x) {
        int result = 0;
        switch (x) {
            case 1:
                result += 1;
            case 2:
                result += 2;
            case 3:
                result += 3;
                break;
            default:
                result -= 1;
        }
        return result;
    }

    private static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
