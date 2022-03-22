public class ComplexAssign {

    public static void main(String[] args) {
        B b1 = new B();
        B b2 = new B();
        B b3 = new B();
        B b4 = b1;
        b4 = b2;
        B b5 = b1;
        b5 = b3;
        b5 = b4;

    }
}

class B {
}
