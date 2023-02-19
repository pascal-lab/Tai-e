class ComplexTaint {
    public static void main(String args[]) {
        String s1 = new String();
        s1 = SourceSink.source();
        String s2 = s1;
        String s3 = s2;
        SourceSink.sink(s3);


        String s4 = new String();
        String s5 = s4;
        s5 = SourceSink.source();
        String s6 = s5;
        SourceSink.sink(s6);

        s5 = s2.concat(new String());
    }
}
