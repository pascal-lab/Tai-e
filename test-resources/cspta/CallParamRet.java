class CallParamRet {

    public static void main(String[] args) {
        A a = new A();
        a.param();
        B b = a.id(new B());
    }

}

class A {

    void param() {
        B b1 = new B();
        B b2 = new B();
        foo(b1, b2);
        bar(b2, b1);
    }

    void foo(B p1, B p2) {
    }

    void bar(B p1, B p2) {
    }

    B id(B b) {
        return b;
    }
}

class B {
}
