class BinaryOp {

    void constant1() {
        int i0 = 0, i1 = 1, i2 = 2;
        int x = 1 + 2;
        int y = i0 + 3;
        int z = i1 + i2;
    }

    void constant2() {
        int i1 = 1;
        int x = i1 + 10;
        int y = x * 5;
        int z = y - 12;
    }

    void NAC(int p) {
        int x = p;
        int y = 1 + x;
    }

    void undefined() {
        int x;
        int y = x + 1;
    }

    void longExpressions() {
        int x = 1, y = 2, z = 3;
        int a = x + y * z;
        int b = (x - y) * z;
    }

    void shift(int x) {
        int a = x << 5;
        int b = 3;
        int z = b << 5;
    }

    void bitwise(int x) {
        int a = 1023;
        int b = a ^ 8;
        int c = a | x;
        int d = 65535 & a;
    }
}
