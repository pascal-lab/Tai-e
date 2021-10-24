interface I {
    abstract void foo();
}

interface II extends I {
    abstract void bar();
}

interface III extends I {
    abstract void bar();
}

public class Interface3 {
    public static void main(String[] args) {
        I i = new D();
        i.foo();

        II ii = new D();
        ii.foo();
        ii.bar();

        III iii = new D();
        iii.bar();
        iii.foo();

    }
}


class A implements II {
    public void foo() {
    }

    public void bar() {
    }
}

class B implements III {
    public void foo() {

    }

    public void bar() {
    }
}

class D extends A implements III {

}