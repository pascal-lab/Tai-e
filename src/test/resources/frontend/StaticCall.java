class StaticCall {
    private static int f() {
        return 10;
    }

    public int g() {
        int x = f();
        if (x > 0) {
            return x + 1;
        }
        return f() + 10;
    }
}