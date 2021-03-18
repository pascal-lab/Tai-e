class NotDead {

    void foo(int x, int y) {
        int a;
        if (x > y) {
            a = 0;
        } else {
            a = 1;
        }
        use(a);
    }

    int bar() {
        int x = 1;
        int y = x;
        int z = y;
        return z;
    }

    void use(int n) {
    }
}
