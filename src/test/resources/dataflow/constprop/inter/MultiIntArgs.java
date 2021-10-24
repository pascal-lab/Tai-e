public class MultiIntArgs {

    static int goo(int x, int y) {
        return (x + y);
    }

    static int foo(int x, int y) {
        return (x * y);
    }

    public static void main(String[] args) {
        //call goo once
        int a = 2;
        int b = 3;
        int c = goo(a, b);

        //call foo twice with different args
        int x = 2;
        int y = 3;
        int z = foo(x, y);

        int r = 4;
        int s = 5;
        int t = foo(r, s);

    }
}
