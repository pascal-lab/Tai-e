class ObjOnly {
    public static void main(String[] args) {
        A a1 = new A();
        A a2 = new A();

        B b1 = new B();
        B b2 = new B();

        C c1 = new C();
        C c2 = new C();

        D d1 = new D();
        D d2 = new D();

        a1.setF(b1, c1, d1);
        a2.setF(b2, c2, d2);
        D result = a1.getF().getG().getH();
    }
}

class A {
    B f;

    void setF(B b, C c, D d) {
        this.f = b;
        this.f.setG(c, d);
    }

    B getF() {
        return this.f;
    }
}

class B {
    C g;

    void setG(C c, D d) {
        this.g = c;
        this.g.setH(d);
    }

    C getG() {
        return this.g;
    }
}

class C {
    D h;

    void setH(D d) {
        this.h = d;
    }

    D getH() {
        return this.h;
    }
}

class D {

}
