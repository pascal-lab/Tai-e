class UnreachableIfBranch {

    int branch1() {
        int x = 10;
        int y = 1;
        int z;
        if (x > y) {
            z = 100;
        } else {
            z = 200; // unreachable branch
        }
        return z;
    }

    void branch2() {
        int x = 10;
        int y = 1;
        int z;
        if (x > y) {
            z = 100;
            use(z);
            return;
        } else {
            z = 200; // unreachable
        }
        int a = x; // unreachable
        use(a); // unreachable
        use(z); // unreachable
    }

    void use(int x) {
    }
}
