public class TestCHA {

    public static void main(String[] args) {
        I i = new C();
        i.foo(null);

        A a = new C();
        a.bar();
    }
}

interface I {
    void foo(Object o);
}

abstract class A {
    abstract void bar();
}

class C extends A implements I {
    
    public void foo(Object o) {
        instanceMethod();
    }

    void bar() {
        staticMethod();
    }

    void instanceMethod() {}

    static void staticMethod() {}
}
