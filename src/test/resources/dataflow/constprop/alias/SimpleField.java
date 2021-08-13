class SimpleField {

    public static void main(String[] args) {
        A a = new A();
        int x = a.f;
        int y = a.g;
        int z = a.h;
        String s = a.s;
    }

    static class A {

        int f = 10;

        int g = 20;

        int h;

        String s = "xxx";
    }
}
