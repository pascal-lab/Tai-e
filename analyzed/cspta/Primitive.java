class Primitive {

    public static void main(String[] args) {
        boolean b = true;
        A a = new A();
        in(a, 1, a);
        int i = 10;
        in(a, i, a);
        int x = out();
    }

    static void in(A a1, int n, A a2) {
    }

    static int out() {
        return 222;
    }
}

class A {
}
