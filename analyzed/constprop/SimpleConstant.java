class SimpleConstant {

    static void constant() {
        int x = 1;
        int y = 2;
        int z = 3;
        x = 100;
    }

    static void propagation() {
        int x = 10;
        int y = x;
        int z = y;
    }

    static void multipleAssigns() {
        int x = 1, y = 10;
        x = 2;
        x = 3;
        x = 4;
    }
}
