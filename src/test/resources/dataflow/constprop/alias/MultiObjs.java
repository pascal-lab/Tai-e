class MultiObjs {

    public static void main(String[] args) {
        A a1 = new A();
        a1.f = 2333;
        A a2 = new A();
        a2.f = 666;
        A a3 = new A();
        a3.f = 666;
        A aa = args.length > 0 ? a1 : a2;
        A aaa = args.length > 0 ? a2 : a3;
        int x = aa.f;
        int y = aaa.f;
        int z = a1.f;
    }
}

class A {
    int f;
}
