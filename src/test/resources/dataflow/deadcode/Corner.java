class Corner {
    int sideEffectAssignment() {
        int x = 1;
        long y = 2;
        x = (int) y;
        return 5;
    }

    void sideEffectMod() {
        int x = 0;
        int y = 1 % x;
    }

    void sideEffectDivide() {
        int x = 0;
        int y = 1 / x;
    }

    int invoke() {
        int x = 1;
        int y = 2;
        x = foo(y);
        return 5;
    }

    int foo(int x) {
        x = 2;
        return x;
    }
}
