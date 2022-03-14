class Cond {
    public void f(int x) {
        int i = (x > 10 || x < 20) ? 10 : 20;
        return;
    }

    public void g(int x) {
        int t = (x > 20 && ! (x < 10)) ?
                ((x < 20 || x > 50) ? 10 : 30) : 40;
        return;
    }
}