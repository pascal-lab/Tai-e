class If {
    public static int f(int x) {
        if (x > 0) {
            return x;
        } else {
            return x * 100;
        }
    }

    public static int g(int x) {
        if (x > 0) {
            return x;
        }
        return x - 1;
    }

    public static int h(int x, int y) {
        if (x == 0 || y == 0) {
            return x - 1;
        } else {
            if (x > 9) {
                boolean b = (x > 120) || (y < 328190);
                return y;
            }
            return x;
        }
    }
}