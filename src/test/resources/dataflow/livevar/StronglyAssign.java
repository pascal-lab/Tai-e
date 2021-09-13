class StronglyAssign {

    int assign(int a, int b, int c) {
        int d = a + b;
        b = d;
        c = a;
        return b;
    }
}
