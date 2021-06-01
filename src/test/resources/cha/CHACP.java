public class CHACP {
    public static void main(String[] args) {
        int a, b, c;
        CHACP x = new CHACP();
        a = 6;
        b = x.addOne(a);
        c = b - 3;
        b = x.ten();
        c = a * b;
    }

    int addOne(int x) {
        int y = x + 1;
        return y;
    }

    int ten() {
        return 10;
    }
}
