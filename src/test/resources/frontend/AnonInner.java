interface Adder {
    int add(int j);
}

class AnonInner {

    static Adder makeAdder(final int i) {
        return new Adder() {
            @Override
            public int add(int j) {
                return i + j;
            }
        };
    }

    static void show() {
        System.out.println(makeAdder(2).add(5));
    }
}