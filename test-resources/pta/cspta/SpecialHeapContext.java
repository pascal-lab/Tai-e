class SpecialHeapContext {

    public static void main(String[] args) {
        A a1 = new A();
        A a2 = new A();
        String s1 = a1.getMyString();
        String s2 = a2.getMyString();
    }
}

class A {

    String getMyString() {
        return "string";
    }
}
