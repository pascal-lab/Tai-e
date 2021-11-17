class TwoCallOnly {
    public static void main(String[] args) {
        B b = new B();
        A a1 = new A(new X());
        A a2 = new A(new X());
        X x1 = a1.fun(b);
        X x2 = a2.fun(b); //pts of x1 and x2?
    }
}

class B {
    X get(X x) {
        return x;
    }
}

class A {
    X x;

    A(X x) {
        this.x = x;
    }

    X fun(B b) {
        return b.get(this.x);
    }
}

class X {
}