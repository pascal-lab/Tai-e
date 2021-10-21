public class InterestingSpecial {

    public static void main(String[] args) {
        B b = new B();
        b.callInteresting();
    }
}

class A {

    private void interesting() {
        System.out.println("A.interesting()");
    }

    void callInteresting() {
        System.out.println(this);
        interesting(); // Since Java 11, invokevirtual/invokeinterface
                       // may be used to call private methods
    }
}

class B extends A {
    void interesting() {
        System.out.println("B.interesting()");
    }
}
