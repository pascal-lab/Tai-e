class BaseToResult {

    public static void main(String[] args) {
        String taint = SourceSink.source();
        String s1 = new String();
        String s2 = taint.concat(s1);
        SourceSink.sink(s2); // taint
    }
}
