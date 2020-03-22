class Simple {
    public static void main(String[] args) {
        constant();
        undefined();
    }

    private static void constant() {
        int x = 1;
        int y = 2;
        int z = 3;
        boolean t = true;
        boolean f = false;
        x = 100;
    }

    private static void undefined() {
        int x, y, z;
        z = 1;
    }
}
