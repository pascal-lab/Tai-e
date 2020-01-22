public class TestDeadCodeElimination {

    void controlFlowUnreachable1() {
        int x = 1;
        int y = 2;
        return;
        int z = 3;
    }

    static void controlFlowUnreachable2() {
        int x = 1;
        int y = 2;
        use(x);
        use(y);
        return;
        int z = 3;
        use(z);
    }

    void deadAssign1() {
        int x = 1;
        int y = x + 2; // dead assignment
        int z = x + 3;
        use(z);
        int a = x; // dead assignment
    }

    void deadAssign2(int p, int q) {
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

    void notDead() {
        Object o = new Object();
        int[] arr = new int[10];
        int b = ten();
    }

    static void use(int x) {}

    int ten() {
        return 10;
    }
}
