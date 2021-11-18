class MultiLoads {

    public static void main(String[] args) {
        A a = new A();
        a.f = 666;
        int x = a.f;
        int y = a.f;
        int z = a.f;
    }
}

class A {
    int f;
}
