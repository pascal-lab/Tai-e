class ForLoops {

    void dead1() {
        int m = 0;
        int y = 2;
        for (int i = 0; i < m; ++i) {
            use(y);
            return;
        }
        use(y)
    }

    void dead2() {
        int m = 0;
        int y = 2;
        for (int i = 0; i >= m; ++i) {
            use(y);
            return;
        }
        use(y)
    }

    void use(int x) {
    }
}