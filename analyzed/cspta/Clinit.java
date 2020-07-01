class Clinit {
    static {
        String s = "Clinit";
    }

    public static void main(String[] args) {
        new A();
        B[][][] barr = new B[1][1][1];
        new D();
        E.f = "x";
        String x = F.f;
        G g;
        H.foo();
        new I();
    }
}

class A {
    static {
        String s = "A";
    }
}

class B {
    static {
        String s = "B";
    }
}

abstract class C {
    static {
        String s = "C";
    }
}

class D extends C {
}

class E {
    static String f;

    static {
        String s = "E";
    }
}

class F {
    static String f;

    static {
        String s = "F";
    }
}

class G {
    static {
        String s = "G";
    }
}

class H {
    static {
        String s = "H";
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
