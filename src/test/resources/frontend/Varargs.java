class Varargs {
    public int f(int a, int ... t) {
        return t[1];
    }

    public int g() {
        return f(10, 1, 2);
    }

    public int k(int a, int[] ... t) {
        return t[0][0];
    }

    public int k2() {
        return k(10, new int[] {1}, new int[]{2});
    }
}