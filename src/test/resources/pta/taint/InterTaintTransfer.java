class InterTaintTransfer {

    public static void main(String[] args) {
        String t1 = SourceSink.source();
        String t2 = SourceSink.source();
        String t3 = SourceSink.source();
        String s = new String();
        SourceSink.sink(transfer(t1, s));
        SourceSink.sink(transfer(t2, s));
        SourceSink.sink(transfer(s, t3));
    }

    static String transfer(String s1, String s2) {
        return s1.concat(s2);
    }
}
