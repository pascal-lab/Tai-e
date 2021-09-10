class DivisionByZero {

    void divideNACByZero(int p) {
        int zero = 0;
        int undef1 = p / zero;
        int undef2 = p % zero;
    }

    void divideConstantByZero() {
        int one = 1;
        int zero = 0;
        int undef1 = one / zero;
        int undef2 = one % zero;
    }
}
