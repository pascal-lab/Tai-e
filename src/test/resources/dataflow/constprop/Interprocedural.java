class Interprocedural {

    void param(int i, boolean b) {
        int x = i;
        int y = i + 10;
        boolean p = b;
        boolean q = b || false;
    }

    void invoke() {
        int x = ten();
        int y = id(10);
    }

    int ten() {
        return 10;
    }

    int id(int x) {
        return x;
    }
}
