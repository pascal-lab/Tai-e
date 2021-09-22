class A {
    C c;

    A() {
        this.c = null;
    }

    void setC(C c) {
        this.c = c;
    }
}

class B extends A {
    B() {
        super();
    }
}

class C {
}

class Reference {
    A referenceType(C c) {
        B b = new B();
        b.setC(c);
        return b;
    }
}
