public class StoreLoad {

    public static void main(String[] args) {
        A a1 = new A();
        B b1 = new B();
        a1.f = b1;
        A a2 = a1;
        B b2 = a2.f;
    }
}

class A {
    B f;
}

class B {
}
