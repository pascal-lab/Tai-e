class UnreachableIfBranch2 {

    void branch() {
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
