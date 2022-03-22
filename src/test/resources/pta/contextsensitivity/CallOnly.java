interface A {
}

class CallOnly {
    public static void main(String args[]) {
        B bb = new B();
        C cc = new C();
        Outer1 outer1 = new Outer1(bb);
        Outer2 outer2 = new Outer2(cc);
        Inner inner = new Inner();
        A result1 = outer1.get(inner);
        A result2 = outer2.get(inner);
    }
}

class Outer1 {
    B b;

    Outer1(B b) {
        this.b = b;
    }

    A get(Inner inner) {
        return inner.identity(this.b);
    }
}

class Outer2 {
    C c;

    Outer2(C c) {
        this.c = c
    }

    ;

    A get(Inner inner) {
        return inner.identity(this.c);
    }
}

class Inner {
    A identity(A a) {
        return a;
    }
}

class B implements A {
}

class C implements A {
}

