class DefUse {

    int assign() {
        int x = 1;
        int y = x + 111;
        x = 2;
        int z = x + y;
        return z;
    }

    int branch(int a, int b, int c) {
        int x;
        if (a > 0) {
            x = a;
        } else {
            x = b;
        }
        int y = x;
        x = c;
        return x;
    }

    int loop(int a, int b) {
        int c;
        while (a > b) {
            c = b;
            --a;
        }
        return c;
    }
}
