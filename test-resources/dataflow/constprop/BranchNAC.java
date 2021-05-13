class BranchNAC {

    void NAC1(boolean b) {
        int x;
        if (b) {
            x = 10;
        } else {
            x = 20;
        }
        int y = x;
    }

    void NAC2(boolean b, int p) {
        int x, undef;
        if (b) {
            x = undef;
        } else {
            x = p;
        }
        int y = x;
    }

    void nonDistributivity(boolean b) {
        int x, y;
        if (b) {
            x = 2;
            y = 3;
        } else {
            x = 3;
            y = 2;
        }
        int z = x + y;
    }
}
