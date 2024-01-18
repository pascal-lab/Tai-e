class Cast {
    public static void main(String[] args) {
        Object o = new A();
        o = new B();
        o = new C();
        A a = (A) o;
        PTAAssert.notEquals(a, o);
        B b = (B) o;
        PTAAssert.notEquals(b, o);
        C c = (C) o;
        PTAAssert.notEquals(c, o);
    }
}

class A {
}

class B {
}

class C extends B {
}
