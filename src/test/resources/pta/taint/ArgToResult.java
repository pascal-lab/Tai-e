class ArgToResult {

    public static void main(String[] args) {
        String taint = SourceSink.source();
        String s1 = new String();
        String s2 = s1.concat(taint);
        SourceSink.sink(s2); // taint
    }
}
