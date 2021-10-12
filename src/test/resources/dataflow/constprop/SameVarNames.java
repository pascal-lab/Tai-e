class SameVarNames {

    void constant1(boolean b) {
        if (b) {
            int a = 1;
        } else {
            int a = 2;
        }
        int a = 3;
    }

    void constant2(boolean b) {
        int a;
        if (b) {
            a = 1;
        } else {
            a = 2;
        }
        a = 3;
    }
}
