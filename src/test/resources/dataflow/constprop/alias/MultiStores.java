class MultiStores {
    public static void main(String[] args) {
        A a = new A();
        if (args.length > 0) {
            a.f = 0;
        } else {
            a.f = 1;
        }
        int x = a.f;
    }
}

class A {
    int f;
}
