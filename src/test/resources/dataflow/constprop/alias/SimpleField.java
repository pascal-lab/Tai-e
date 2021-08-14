class SimpleField {

    public static void main(String[] args) {
        A a = new A();
        a.f = 111;
        int x = a.f;
    }

    static class A {
        int f;
    }
}
