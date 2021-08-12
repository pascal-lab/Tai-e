class SimpleField {

    public static void main(String[] args) {
        A a = new A();
        int x = a.f;
        int y = a.g;
        String s = a.h;
    }

    static class A {

        int f = 10;

        int g = 20;

        String h = "xxx";
    }
}
