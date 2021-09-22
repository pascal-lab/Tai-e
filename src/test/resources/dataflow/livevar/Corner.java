class Corner {
    int exit() {
        return 1;
    }

    void exit2() {
        int a = 1;
    }

    void exit3() {
        ;
    }

    int deadLoop() {
        int a = 1;
        while (true) {
            a += 1;
        }
        return a;
    }

}