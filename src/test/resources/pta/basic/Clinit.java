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

        PTAAssert.reachable("<A: void <clinit>()>",
                "<C: void <clinit>()>",
                "<E: void <clinit>()>",
                "<F: void <clinit>()>",
                "<H: void <clinit>()>",
                "<I: void <clinit>()>",
                "<J: void <clinit>()>");
    }
}

class A {
    static {
    }
}

class B {
    static {
    }
}

abstract class C {
    static {
    }
}

class D extends C {
}

class E {
    static Object f;

    static {
    }
}

class F {
    static Object f;

    static {
    }
}

class G {
    static {
    }
}

class H {
    static {
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
