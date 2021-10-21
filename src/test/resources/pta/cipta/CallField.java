class CallField {

    public static void main(String[] args) {
        A a = new A();
        a.setget();
        a.modifyParam();
    }

}

class A {

    void setget() {
        B b = new B();
        b.set(new C());
        C c = b.get();
    }

    void modifyParam() {
        B b1 = new B();
        B b2 = new B();
        b1.setC(b2);
        C c = b2.c;
    }
}

class B {

    C c;

    void set(C c) {
        this.c = c;
    }

    C get() {
        return c;
    }

    void setC(B b) {
        b.c = new C();
    }
}

class C {
}
