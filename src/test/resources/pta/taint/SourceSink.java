class SourceSink {

    static String tainted1;

    static String untainted;

    String tainted2;

    static String source() {
        return new String();
    }

    static void sink(String s) {
    }

    static void sink(String s, int n) {
    }

    static void sink(String s1, String s2) {
    }

    static String sourceAndSink(String s1, String s2) {
        return new String();
    }
}
