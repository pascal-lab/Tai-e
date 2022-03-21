class SameName {
    public int f(int k) {
        int t = k;
        for (int x = 0; x < 10; ++x) {
            t += x;
        }
        for (int x = 0; x < 10; ++x) {
            t += x;
        }
        return t;
    }
}