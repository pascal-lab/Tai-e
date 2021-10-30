class MergeParam {

    public static void main(String[] args) {
        A a1 = new A();
        A a2 = new A();

        A result = foo(a1);
        result = foo(a2);
    }

    public static A foo(A a) {
        return a;
    }

}

class A {

}

