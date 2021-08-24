public class Hierarchy {
}

interface I {
    void biu(I i);
}

interface II {
    String fii = "fii";
}

interface III extends I, II {
}

interface IIII extends III {
    void baz(boolean b);

    void biubiu(IIII iiii);
}

class C {
    String fc;

    String f;

    void foo(long l) {
    }

    void bar() {
    }

    public void baz(boolean b) {
    }
}

class D extends C {
}

abstract class E extends C implements I, II {
    String fe;

    String f;

    void foo(int i) {
    }
}

abstract class F implements III {
}

abstract class G extends E implements IIII {
}

abstract class H extends F {
}
