class Dispatch {
    public static void main(String[] args) {
        A a = new A();
        a = new B();
        a = new C();
        T t = a.foo();
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