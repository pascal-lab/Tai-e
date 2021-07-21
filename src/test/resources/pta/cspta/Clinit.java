interface K {
    A f = new A();
}

class Clinit {
    static {
        new Object();
    }

    public static void main(String[] args) {
        new A();
        B[][][] barr = new B[1][1][1]; // newarray doesn't trigger <clinit>
        new D();
        E.f = new Object();
        Object o = F.f;
        G g;
        H.foo();
        new I();
        // A a = K.f;
        // By JLS, this does not trigger initialization of interface K,
        // as K does not declare default methods.
        new L();
    }
}

class A {
    static {
        new Object();
    }
}

class B {
    static {
        new Object();
    }
}

abstract class C {
    static {
        new Object();
    }
}

class D extends C {
}

class E {
    static Object f;

    static {
        new Object();
    }
}

class F {
    static Object f;

    static {
        new Object();
    }
}

class G {
    static {
        new Object();
    }
}

class H {
    static {
        new Object();
    }

    static void foo() {
    }
}

class I {
    static J f = new J();
}

class J {
    static I f = new I();
}

class L implements K {
}
