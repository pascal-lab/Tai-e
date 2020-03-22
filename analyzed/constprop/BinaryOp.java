class BinaryOp {

    void constant() {
        int i0 = 0, i1 = 1, i2 = 2;
        int x = 1 + 2;
        int y = i0 + 3;
        int z = i1 + i2;
    }

    void NAC(int p) {
        int x = p;
        int y = 1 + x;
    }

    void undefined() {
        int x;
        int y = x + 1;
    }
}
