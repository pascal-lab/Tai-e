public class ControlFlowUnreachable2 {

    static void foo() {
        int x = 1;
        int y = 2;
        use(x);
        use(y);
        return;
        int z = 3; // unreachable
        use(z); // unreachable
    }

    static void use(int x) {
    }
}
