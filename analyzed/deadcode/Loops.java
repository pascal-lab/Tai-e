class Loops {

    void deadLoop() {
        int x = 1;
        int y = 0;
        int z = 100;
        while (x > y) {
            use(z);
        }
        dead(); // unreachable branch
    }

    void dead() {
    }

    void use(int n) {
    }
}
