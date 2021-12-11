class CycleTaint {
    public static void main(String args[]) {
        String s1 = new String();
        SourceSink.sink(s1);

        String s2 = s1;
        String s3 = s2;
        s1 = s3;

        s3 = SourceSink.source();
    }

}