public class SimpleUnreachable {

    static int foo() {
        int x = 1;
        return x;
        dead(); // unreachable
    }

    static void dead() {}
}
