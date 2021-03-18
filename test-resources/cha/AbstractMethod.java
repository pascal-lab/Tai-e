public class AbstractMethod {

    public static void main(String[] args) {
        A a = new B();
        a.foo();
    }
}

abstract class A {
    abstract void foo();
}

class B extends A {
    void foo() {
    }
}
