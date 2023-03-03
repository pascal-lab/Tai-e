class OneCall {
    public static void main(String[] args) {
        C c = new C();
        c.m();
    }
}

class C {

    void m() {
        Number n1, n2, x, y;
        n1 = new One();
        n2 = new Two();
        x = this.id(n1);
        y = this.id(n2);
        int i = x.get(); // x -> new One, i = 1
    }

    Number id(Number n) {
        return n;
    }
}

interface Number {
    int get();
}

class Zero implements Number {
    public int get() {
        return 0;
    }
}

class One implements Number {
    public int get() {
        return 1;
    }
}

class Two implements Number {
    public int get() {
        return 2;
    }
}
