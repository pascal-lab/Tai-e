class Obj1 {
    private int x;
    private A y;

    public Obj1(int x, A y) {
        this.x = x;
        this.y = y;
    }

    public int f() {
        x = 1000 * 0123;
        A.ASF = 231;
        y.h = 123;
        return x * y.hashCode() * y.f();
    }
}

class A {
    public static int ASF = 123123;
    public int h;
    public int f() {
        return ASF;
    }
}