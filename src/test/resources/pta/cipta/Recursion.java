public class Recursion {
    public static void main(String[] args) {
        A a = new A();
        B b = new B();
        B b5 = a.foo(b);

    }


}

class A {
    public B foo(B b) {
        B b1 = b;
        B b2 = goo(b1);
        return b2;
    }

    public B goo(B b) {
        B b3 = b;
        B b4 = foo(b3);
        return b4;
    }
}

class B {

}