class ComplexAssign {

    int complexAssign(int a, int b) {
        int x = 1;
        x = 2;
        a = x;
        a = a - 1;
        x = x - 1;
        x = 2;
        b = a;
        b = b - 1;
        b = x - 2;
        return b;
    }
}