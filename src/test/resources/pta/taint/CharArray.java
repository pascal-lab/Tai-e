class CharArray {

    public static void main(String[] args) {
        String s1 = SourceSink.source();
        char[] chars = s1.toCharArray();
        String s2 = new String(chars);
        SourceSink.sink(s2); // taint

        String s3 = new String();
        SourceSink.sink(s3);
    }
}
