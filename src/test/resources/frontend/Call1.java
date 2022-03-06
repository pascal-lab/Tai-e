public class Call1 {
    public int f() {
        A a = new A(10);
        return a.f(100);
    }
}

class A {
    public A(int i) {}
    public int f(int i) {
        return i;
    }
}