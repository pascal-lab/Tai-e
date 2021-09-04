class Invoke {

    int invoke(int a, int b, int c) {
        int x = a - b;
        int y = a - x;
        int z = x;
        invoke(0, 0, 0);
        return z;
    }
}
