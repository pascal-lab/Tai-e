public class MultiReturn {
    boolean retDiffConst(int x) {
        if (x % 2 == 0)
            return true;
        return false;
    }

    double retDouble(int x) {
        if (x % 2 == 0)
            return 1.0;
        return 0.0;
    }

    int retX(int x) {
        if (x % 2 == 0)
            return 1;
        return x;
    }

    int retUndef(int y) {
        if (y % 2 == 0)
            return 1;
        return y;
    }

    public static void main(String[] args) {
        MultiReturn mr = new MultiReturn();
        int x = 0;
        mr.retDiffConst(x);
        mr.retDouble(x);
        x = 1;
        mr.retX(x);
        int y;
        mr.retUndef(y);
    }
}