class SpecialHeapContext {

    public static void main(String[] args) {
        A a1 = new A();
        A a2 = new A();
        Object o1 = a1.getMyObject(); o1.hashCode();
        Object o2 = a2.getMyObject(); o2.hashCode();
    }
}

class A {

    Object getMyObject() {
        return new Object();
    }
}
