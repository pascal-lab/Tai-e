public class TestCHA {

    public static void main(String[] args) {
        I i = new C();
        i.foo(null);

        A a = new C();
        a.bar();

        Number n = new One();
        n.get();
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


interface Number {
    int get();
}

class Zero implements Number {
    @Override
    public int get() {
        return 0;
    }
}

class One implements Number {
    @Override
    public int get() {
        return 1;
    }
}

class Two implements Number {
    @Override
    public int get() {
        return 2;
    }
}
