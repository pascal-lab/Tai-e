class StaticField {

    public static void main(String[] args) {
        A.b = new B();
        B b = A.b;
    }

}

class A {
    static B b;
}

class B {
}
