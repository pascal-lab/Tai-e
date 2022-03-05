class Loop {
    public void f(int x) {
        int temp = 0;
        while (x > 0) {
            if (x == 0) {
                break;
            }
            if (temp < 0) {
                continue;
            }
            temp = temp + 1;
        }
        return;
    }

    public int g(int x) {
        if (x > 0 == x < 0) {
            while (x == 0) {
                boolean b = (x < 0) || (x > 0);
            }
        }
        return 0;
    }

    public int h(int x) {
        L:
        while (x > 0) {
            x = x + 1;
            while (x < 0) {
                x = x - 1;
                if (x == 0) {
                    break L;
                }
                continue L;
            }
        }
        return 10;
    }

    public void k() {
        int x = 0;
        while (x > 0) {
            if (x > 10) {
                continue;
            }
            break;
        }
        return;
    }

    public void h1() {
        int x = 0;
        do {
            x = x + 1;
        } while (x < 10000);
        return;
    }

    public void g1() {
        int x = 0;
        int temp = 123;
        do {
            while (temp > 0) {
                temp = x - temp * 123 + 123123;
                break;
            }
            break;
        } while (! (temp < 0) );
        return;
    }
}