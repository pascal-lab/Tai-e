interface I {
    void foo();
}

interface II {
    void foo();

    void bar();
}

public class Interface2 {

    public static void main(String[] args) {
        I i = new A();
        i.foo();
        II ii = new E();
        ii.foo();
        ii.bar();
    }
}

class A implements I {
    public void foo() {
    }
}

class B extends A {
    public void foo() {
    }
}

class C extends A {
    public void foo() {
    }
}

class D implements I {
    public void foo() {
    }
}

class E implements I, II {
    public void foo() {
    }

    public void bar() {
    }
}
