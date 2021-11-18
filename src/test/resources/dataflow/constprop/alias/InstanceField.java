class InstanceField {

    public static void main(String[] args) {
        A a1 = new A();
        a1.f = 111;
        int x = a1.f;
        A a2 = new A();
        a2.f = 222;
        int y = a2.f;
    }
}

class A {
    int f;
}
