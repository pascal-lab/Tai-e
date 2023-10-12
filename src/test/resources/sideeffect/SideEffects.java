/*
 * This testcase is taken from https://github.com/saffriha/ictac2014
 */

class IntPair { public int x; public int y; }

public class SideEffects {
    public static void assignFields(IntPair p) { p.x = p.y; }
    public static void nonRefParams(int a, IntPair p, int unused) { p.x = a; }
    public static int binop(int a, int b, int unused) { return a + b; }

    ////// Entry function //////////////////////////////////////////////////////
    public static void main(String[] args) {
        IntPair p = new IntPair();

        assignFields(p);
        nonRefParams(0, p, 1);
        binop(1, 2, 3);
    }

}