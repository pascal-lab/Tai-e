class FieldCorner {
    public static void main(String args[]) {
        corner1();
        corner2();
        corner3();
        corner4();
    }

    public static void corner1() {
        A a1 = new A();
        A a2 = a1;
        A a3 = a2;
        a1.f = 555;
        int r1 = a3.f;
        a2.f = 666;
    }

    public static void corner2() {//one obj with two fields
        A b1 = new A();
        b1.f = 1;
        b1.g = 2;
        A b2 = b1;
        int r2 = b2.g;

    }

    public static void corner3() {//c1 is null pointer
        A c1;
        c1.f = 2;
        int r3 = c1.f;
    }

    public static void corner4() {//pts overlap
        A d1 = new A();
        A d2 = new A();
        A d3 = new A();

        A d4 = d1;
        d4 = d2;
        A d5 = d3;
        d5 = d2;
        d4.f = 1;
        int r4 = d5.f;
    }
}

class A {
    int f;
    int g;
}
