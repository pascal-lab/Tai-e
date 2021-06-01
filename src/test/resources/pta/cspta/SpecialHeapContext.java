class SpecialHeapContext {

    public static void main(String[] args) {
        A a1 = new A();
        A a2 = new A();
        Object o1 = a1.getMyObject();
        Object o2 = a2.getMyObject();
    }
}

class A {

    Object getMyObject() {
        return new Object();
    }
}
