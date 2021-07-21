/**
 * Test case for reaching definition analysis.
 */
class ReachDef {

    int foo(int a, int b, int c) {
        int x;
        if (a > 0) {
            x = a;
        } else {
            x = b;
        }
        int y = x;
        x = c;
        return x;
    }

    int loop(int a, int b) {
        int c;
        while (a > b) {
            c = b;
            --a;
        }
        return c;
    }
}
