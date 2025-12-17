class Cast {
    public static void main(String[] args) {
        Object o;
        if (args.length == 0) {
            o = new A();
        } else if (args.length == 1) {
            o = new B();
        } else {
            o = new C();
        }
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
