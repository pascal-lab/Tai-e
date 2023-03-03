class Primitives {

    public static void main(String[] args) {
        int i100 = 100;
        int i101 = 101;
        int sum = i100 + i101;

        A a1 = new A();
        int x = a1.i1;
        A a2 = new A();
        a2.i2 = 666;
        int y = a2.i2;

        double z = id(3.14159);
    }

    static double id(double n) {
        return n;
    }
}

class A {

    int i1 = 333;

    int i2;
}
