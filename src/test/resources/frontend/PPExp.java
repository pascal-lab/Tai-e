class PPExp {
    public int f() {
        int j = 10;
        for (int i = 0; i < 10; ++i) {
            j = j + i;
        }
        return j--;
    }

    public int g() {
        char x = '\u0001';
        int j = ~x;
        int q = -j;
        int k = ~(+q - 123);
        int qq = -(Integer.valueOf(q));
        return j++;
    }
}