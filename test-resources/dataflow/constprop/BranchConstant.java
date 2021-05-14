class BranchConstant {

    void constant1(boolean b) {
        int x = 2;
        int y = 2;
        int z;
        if (b) {
            z = x + y;
        } else {
            z = x * y;
        }
        int n = z;
    }

    void constant2(boolean b) {
        int x;
        if (b) {
            x = 10;
        }
        int y = x;
    }

    void constant3(int i) {
        int x;
        if (i == 10) {
            x = i + 1;
        }
        int y = x;
    }

    void constant4(int i) {
        int a = 0;
        int b = 1;
        int c;
        if (i == 0) {
            if (a == b) {
                c = a;
            }
        } else {
            c = 20;
        }
        int d = c; // c = 20
    }

    void constant5() {
        int a = 0;
        int b = 1;
        int c;
        if (a == b) {
            c = 111;
        } else {
            c = 222;
        }
        int d = c; // c = 222
    }

    void constant6(int i) {
        int a, b, c;
        switch (i) {
            case 1:
                a = i;
                break;
            case 10:
                b = i;
                break;
            default:
                c = i;
        }
        int x = a;
        int y = b;
        int z = c;
    }
}
