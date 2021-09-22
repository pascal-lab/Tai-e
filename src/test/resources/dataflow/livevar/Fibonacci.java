class Fibonacci {
    int getFibonacci(int n) {
        if ((n == 0) || (n == 1)) {
            return n;
        } else {
            return getFibonacci(n - 1) + getFibonacci(n - 2);
        }
    }

}