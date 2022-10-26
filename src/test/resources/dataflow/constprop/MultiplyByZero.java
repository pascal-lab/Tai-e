class MultiplyByZero {

    void multiplyNACByZero(int p) {
        int zero = 0;
        int zero1 = zero * p;
        int zero2 = p * zero;
    }

    void multiplyConstantByZero() {
        int one = 1;
        int zero = 0;
        int zero1 = zero * one;
        int zero2 = one * zero;
    }

    void multiplyUndefByZero() {
        int undef;
        int zero = 0;
        int undef1 = zero * undef;
        int undef2 = undef * zero;
    }
}
