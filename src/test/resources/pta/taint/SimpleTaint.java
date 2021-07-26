class SimpleTaint {

    public static void main(String[] args) {
        String s = source();
        sink(s);
    }

    static String source() {
        return new String();
    }

    static void sink(String s) {
    }
}
