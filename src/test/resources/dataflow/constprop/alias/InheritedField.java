class InheritedField {
    public static void main(String[] args) {
        corner1();
        corner2();
    }

    static void corner1() {
        B b = new B();
        b.setSuperF(22);
        int x = b.f;
        int y = foo(b);
    }

    static int foo(A a) {
        return a.f;
    }

    static void corner2() {
        A a = new A();
        a.f = 22;
        B b = new B();
        b.f = 33;
        bar(a);
        int x = b.f;
        int y = bar(b);
    }

    static int bar(A a) {
        return a.f;
    }


}

class A {
    int f;
}

class B extends A {
    int f;

    void setSuperF(int x) {
        super.f = x;
    }
}