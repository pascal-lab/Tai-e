class ConditionOp {

    void intEqNeq() {
        int i0 = 0, i1 = 1;
        boolean b1 = i0 == i1;
        boolean b2 = i0 != i1;
    }

    void booleanEqNeq() {
        boolean t = true, f = false;
        boolean b1 = t == f;
        boolean b2 = t != f;
    }

    void intGeGt() {
        int i0 = 0, i1 = 1;
        boolean b1 = i0 >= i1;
        boolean b2 = i0 > i1;
    }

    void intLeLt() {
        int i0 = 0, i1 = 1;
        boolean b1 = i0 <= i1;
        boolean b2 = i0 < i1;
    }
}
