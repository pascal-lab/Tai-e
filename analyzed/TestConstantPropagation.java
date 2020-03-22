public class TestConstantPropagation {
    
    // ----- test constant -----
    void boolOp() {
        boolean bTrue = true;
        boolean bFalse = false;
        boolean b1 = bTrue || bFalse;
        boolean b2 = bTrue && bFalse;
        int x = 10;
        int y = 20;
        boolean b3 = x > y;
    }

    // ----- test NAC -----
    void branchNAC(boolean b) {
        int x;
        if (b) {
            x = 10;
        } else {
            x = 20;
        }
        int y = x;
    }

    void branchCond() {
        int x = 10;
        int y = 20;
        int z;
        if (x < y) {
            z = 10;
        }
        int a = z;
    }

    void param(int i, boolean b) {
        int x = i;
        int y = i + 10;
        boolean p = b;
        boolean q = b || false;
    }

    void invoke() {
        int x = ten();
        int y = id(10);
    }

    int ten() {
        return 10;
    }

    int id(int x) {
        return x;
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

    // ----- test UNDEF -----
    void undefined(boolean b) {
        int undef;
        int x = undef;
        int y;
        if (b) {
            y = 10;
        } else {
            y = undef;
        }
        int z = y;
        x = 20;
        int a = x;
    }
}
