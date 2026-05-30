class Reference {

    static int one() {
        return 1;
    }

    static int zero() {
        return 0;
    }

    public static void main(String[] args) {
        Object[] a = new Object[one()];
        a[zero()] = new Reference();
        Object o = a[zero()];
    }
}
