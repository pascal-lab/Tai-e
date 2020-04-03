class SimpleConstant {

    static void constant() {
        int x = 1;
        int y = 2;
        int z = 3;
    }

    static void propagation() {
        int x = 10;
        int y = x;
        int z = y;
    }
}
