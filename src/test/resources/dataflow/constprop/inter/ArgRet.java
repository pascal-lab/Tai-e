class ArgRet {

    public static void main(String[] args) {
        int n1 = branch(args.length);
        int n2 = loop(6);
    }

    static int branch(int x) {
        int n;
        if (x > 0) {
            n = 111;
        } else {
            n = -666;
        }
        n = id(n);
        return n;
    }

    static int id(int x) {
        return x;
    }

    static int loop(int times) {
        int n = 2333;
        int r = 2334;
        for (int i = 0; i < times; ++i) {
            n = add1(n);
            r = n;
        }
        return r;
    }

    static int add1(int x) {
        return x + 1;
    }
}
