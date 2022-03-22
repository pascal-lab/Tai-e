class StaticSelect {
    public static void main(String[] args) {
        new Int(4).fact();
    }
}

class Int {
    int v;

    Int(int n) {
        this.v = n;
    }

    int fact() {
        return staticFact(this);
    }

    static int staticFact(Int n) {
        if (n.v <= 0) {
            return 1;
        }
        return n.v * staticFact(new Int(n.v - 1));
    }
}