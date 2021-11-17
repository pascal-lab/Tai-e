class MustUseHeap {
    public static void main(String[] args) {
        A a1 = createA();
        a1.f = new B();
        A a2 = createA();
        a2.f = new B();
        B result = a1.f;
    }

    public static A createA() {
        return new A();
    }
}

class A {
    B f;
}

class B {
}