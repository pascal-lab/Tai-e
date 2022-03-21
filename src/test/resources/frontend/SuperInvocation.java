class T {
    public int g() {
        return 10;
    }

    public int f() {
        return 20;
    }
}

class SuperInvocation extends T {
    public int g() {
        return super.g() + f();
    }
}