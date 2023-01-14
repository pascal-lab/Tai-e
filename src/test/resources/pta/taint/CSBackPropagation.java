class CSBackPropagation {

    private char[] buffer = new char[100];

    public static void main(String[] args) {
        CSBackPropagation csbp1 = new CSBackPropagation();
        String taint1 = SourceSink.source();
        String s1 = csbp1.transfer(taint1);
        SourceSink.sink(s1);

        CSBackPropagation csbp2 = new CSBackPropagation();
        String noTaint = "no taint";
        String s2 = csbp2.transfer(noTaint);
        SourceSink.sink(s2);

        CSBackPropagation csbp3 = new CSBackPropagation();
        String taint2 = SourceSink.source();
        String s3 = csbp3.transfer(taint2);
        SourceSink.sink(s3);
    }

    String transfer(String s) {
        s.getChars(0, s.length(), buffer, 0);
        return new String(buffer);
    }
}
