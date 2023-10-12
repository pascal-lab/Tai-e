public class PrimitiveTest {

    static void pure(A a) {
    }

    public static void main(String[] args) {
        A a = new A(1);
        pure(a);
        a.mod();
        pure(a);
    }
}

class A {
    int a;

    A(int a) {
        this.a = a;
    }

    public void mod() {
        a++;
    }
}