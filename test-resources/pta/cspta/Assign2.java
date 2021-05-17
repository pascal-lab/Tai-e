class Assign2 {
    public static void main(String[] args) {
        new A().cycle();
    }
}

class A {

    void cycle() {
        A a1 = new A();
        A a2 = new A();
        A a3 = new A();
        a1 = a2;
        a2 = a3;
        a3 = a1;
    }
}
