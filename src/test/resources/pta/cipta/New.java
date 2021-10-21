public class New {

    public static void main(String[] args) {
        A a = new A();
        B b1 = new B();
        B b2 = new C();
        C c = new C();
    }
}

class A {
}

class B {
}

class C extends B {
}
