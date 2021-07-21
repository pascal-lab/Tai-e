public class StaticCall {

    public static void main(String[] args) {
        foo();
        A.baz();
    }

    static void foo() {
        bar();
    }

    static void bar() {
    }
}

class A {
    static void baz() {
        B.qux();
    }
}

class B {
    static void qux() {
        A.baz();
    }
}
