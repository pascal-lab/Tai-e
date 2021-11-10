class Reference {

    public static void main(String[] args) {
        Object[] a = new Object[1];
        a[0] = new Reference();
        Object o = a[0];
    }
}
