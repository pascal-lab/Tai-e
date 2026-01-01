class Interprocedural {

    public static void main(String[] args) {
        A a = new A();
        a.f = 555;
        int x = getF(a);
        if (a.f > 0) {
            x = getF(a);
        } else {
            x = getF(a);
        }
    }

    static int getF(A a) {
        return a.f;
    }
}

class A {
    int f;
}
