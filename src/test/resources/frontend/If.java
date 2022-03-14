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

    public static int k(int x) {
        if (! (x < 0))  {
            return x - 1;
        }
        return x;
    }

    public int f1(int x) {
        if (x > 0) {
            return x;
        } else if (x == 0) {
            return 1;
        } else  {
            return 100;
        }
    }

    public int f2(int x) {
        if (x > 0 && (! (x < 10) || x > 20)) {
            return 20;
        } else {
            boolean b = x < 10 && (!(x < 20) || x > 30);
            return 0;
        }
    }
}