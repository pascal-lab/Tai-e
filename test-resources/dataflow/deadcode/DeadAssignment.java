class DeadAssignment {

    static void foo() {
        int x = 1;
        int y = x + 2; // dead assignment
        int z = x + 3;
        use(z);
        int a = x; // dead assignment
    }

    static void use(int n) {
    }
}
