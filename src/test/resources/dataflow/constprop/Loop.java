class Loop {
    void whileConstant() {
        int a, b = 1, c = 1;
        int i = 0;
        while (i < 10) {
            a = b;
            b = c;
            c = 1;
            ++i;
        }
    }

    void whileNAC() {
        int a, b = 0, c = 0;
        int i = 0;
        while (i < 10) {
            a = b;
            b = c;
            c = 1;
            ++i;
        }
    }

    void whileUndefinedConstant() {
        int a, b, c;
        int i = 0;
        while (i < 10) {
            a = b;
            b = c;
            c = 1;
            ++i;
        }
    }
}
