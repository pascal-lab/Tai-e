public class Switch {

    public static void main(String[] args) {
        new Switch().switchStmt(0);
    }

    int switchStmt(int x) {
        switch (x) {
            case 1:
                int y = 5 + x;
                return y;
            case 10:
                foo(10);
                return 500;
            case 100:
                foo(x);
                return 1000;
            default:
                bar(1, null);
        }
        x = x * 2;
        switch (x) {
            case 1:
                int y = 5 + x;
                return y;
            case 2:
                foo(10);
                return 500;
            case 5:
                foo(x);
                return 1000;
            default:
                bar(1, null);
        }
        return 0;
    }

    static void bar(int x, Object o) {
    }

    int foo(int x) {
        return 20;
    }

    int foo(int x, int y, int z) {
        return 30;
    }
}