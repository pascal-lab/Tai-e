class BranchUndef {

    void undefined1(boolean b) {
        int x, undef;
        if (b) {
            x = undef;
        } else {
            x = 10;
        }
        int y = x;
    }

    void undefined2(boolean b) {
        int undef;
        int x = undef;
        x = 20;
        int a = x;
    }
}
