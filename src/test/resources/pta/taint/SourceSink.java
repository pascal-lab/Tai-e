class SourceSink {

    static String tainted1;

    static String untainted;

    String tainted2;

    static String source() {
        return new String();
    }

    static Sanitizer sourceS() {
        return new Sanitizer();
    }

    static void sink(String s) {
    }

    static void sink(String s, int n) {
    }

    static void sink(String s1, String s2) {
    }

    static void sink(Sanitizer s) {
    }

    static String sourceAndSink(String s1, String s2) {
        return new String();
    }
}
