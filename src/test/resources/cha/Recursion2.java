interface I {
    abstract void foo();
}

interface II {
    abstract void bar();
}

public class Recursion2 {
    public static void main(String args[]) {
        I i = new B();
        i.foo();
    }
}

abstract class A implements I {
    public void foo() {
        goo();
    }

    abstract void goo();
}

class B extends A implements II {
    public void bar() {
        super.foo();
    }

    void goo() {
        bar();
    }
}