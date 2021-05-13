public class Assign {

    public static void main(String[] args) {
        A a1 = new A();
        A a2 = a1;
        A a3 = a1;
        B b = new B();
        a1 = b;
    }
}

class A {
}

class B extends A {
}
