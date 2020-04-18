class DeadAssignment2 {

    void deadAssign1() {
        Object o = new Object();
        int b = ten();
    }

    void deadAssign2() {
        int x = 1;
        int y = x + 2; // dead assignment
        int z = x + 3;
        use(z);
        int a = x; // dead assignment
    }

    void deadAssign3(int p, int q) {
        int x, y;
        if (p > q) {
            x = 1;
            y = 10; // dead assignment
        } else {
            x = 0;
        }
        y = 200; // dead assignment
        use(x);
    }

    static void use(int x) {}

    int ten() {
        return 10;
    }
}
