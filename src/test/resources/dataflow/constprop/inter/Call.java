public class Call {

    public static void main(String[] args) {
        A a = new A();
        int x = a.identity(100);
        x = a.identity(200);
        int y = a.constant();
        a.foo(20);
        branch();
        multipleReturn();
        loop1();
    }

    static void branch() {
        int x;
        if (anyBool()) {
            x = 5;
        } else {
            x = five();
        }
        int y;
        if (anyBool()) {
            y = 10;
        } else {
            y = five();
        }
        int z = y;
    }

    static void multipleReturn() {
        int x = five();
        int y = five();
        int z = x * y;
    }

    static int five() {
        return 5;
    }

    static boolean anyBool() {
        getBool(true);
        return getBool(false);
    }

    static boolean getBool(boolean b) {
        return b;
    }

    static void loop1() {
        boolean b = anyBool();
        int x, y, z;
        while (b) {
            x = y;
            y = z;
            z = 1;
        }
    }
}

class A {

    void foo(int p) {
        int q = ten();
        int x = p + q;
        use(x);
    }

    int ten() {
        return 10;
    }

    void use(int x) {
    }

    int identity(int x) {
        return x;
    }

    int constant() {
        return 123;
    }
}

class B extends A {

    @Override
    int constant() {
        return 111;
    }
}
