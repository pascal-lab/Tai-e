class Null {

    public static void main(String[] args) {
        A a = new A();
        foo(a, null);
        A x = returnNull();
        A y = null;
    }

    static void foo(A a1, A a2) {
    }

    static A returnNull() {
        return null;
    }
}

class A {
}
