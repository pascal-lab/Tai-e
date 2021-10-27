class Example {

    public static void main(String[] args) {
        A a = new A();
        A b = new B();
        A c = b.foo(a);
    }
}

class A {
    A foo(A x) {
        return null;
    }
}

class B extends A {
    A foo(A y) {
        A r = new A();
        return r;
    }
}
