class LongObjContext {

    public static void main(String[] args) {
        new FactCalculator(5).result();
    }

}

class FactCalculator {
    int n;

    FactCalculator(int n) {
        this.n = n;
    }

    int result() {
        if (n <= 0) {
            return 1;
        }
        return n * new FactCalculator(n - 1).result();
    }
}