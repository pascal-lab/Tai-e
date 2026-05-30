class PlusPlus {
    public static void main(String[] args) {
        test1(0, 1);
        test2(1, 0);
    }

    static void test1(int x, int y) {
        x = 1;
        ++x;
        y = ++x;
        use(y);
    }

    static void test2(int x, int y) {
        x = 1;
        ++x;
        x++;
        y = ++x;
        use(y);
        y = x++;
        use(y);
    }

    public static void use(int n) {}

}
