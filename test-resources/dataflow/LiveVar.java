public class LiveVar {

    public static void main(String[] args) {
    }

    int foo(int a, int b, int c) {
        int d = a + b;
        b = d;
        c = a;
        return b;
    }

    int bar(int a, int b, int c) {
        int x = a - b;
        int y = a - x;
        int z = x;
        foo(0, 0, 0);
        return z;
    }
}