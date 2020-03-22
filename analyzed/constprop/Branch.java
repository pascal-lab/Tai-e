class Branch {
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

    void NAC1(boolean b) {
        int x;
        if (b) {
            x = 10;
        } else {
            x = 20;
        }
        int y = x;
    }
}
