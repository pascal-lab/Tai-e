class SimpleTaint {

    public static void main(String[] args) {
        String s1 = SourceSink.source();
        SourceSink.sink(s1);

        String s2 = SourceSink.source();
        SourceSink.sink(s2);

        String s3 = args == null ? s1 : s2;
        SourceSink.sink(s3, 0);
    }
}
