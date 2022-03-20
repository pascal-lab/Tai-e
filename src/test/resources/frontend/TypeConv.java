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

    public char f(int x) {
        char a = (char)1.1;
        System.out.println((char)a);
        long b = new Integer(1000);
        float f = x;
        Double d = (double) 100;
        int i = (int) '\u0004';
        return (char) x;
    }

    public void g(Object o) {
        TypeConv t = (TypeConv) o;
    }
}