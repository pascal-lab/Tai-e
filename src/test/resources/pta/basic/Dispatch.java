class Dispatch {

    public static void main(String[] args) {
        A a;
        if (args.length == 1) {
            a = new A();
        } else if (args.length == 2) {
            a = new B();
        } else {
            a = new C();
        }
        T t = a.foo();
        PTAAssert.calls("<A: T foo()>", "<B: T foo()>", "<C: T foo()>");
    }
}

class A {
    T foo() {
        return new T(1);
    }
}

class B extends A {
    T foo() {
        return new T(2);
    }
}

class C extends A {
    T foo() {
        return new T(3);
    }
}

class T {
    int x;

    T(int x) {
        this.x = x;
    }
}
