class TypeConv {
    public void f(int x, int y) {
        Boolean b = true;
        Integer i = 10;

        Integer k = 10 + Integer.valueOf(123123);
    }

    public void g() {
        byte b = 2;
        int a[] = new int[b]; // dimension expression promotion
        char c = '\u0001';
        a[c] = 1; // index expression promotion
    }
}