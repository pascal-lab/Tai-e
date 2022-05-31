class ConstVar {

    int primitiveConst() {
        int x = 1 + 2;
        int y = 3 * x;
        use(666);
        return y;
    }

    void stringConst() {
        use("a string constant");
        use("another string constant");
    }

    void nullConst() {
        use(null);
    }

    void use(int x) {
    }

    void use(Object o) {
    }
}
