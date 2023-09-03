class Java9StringConcat {

    public static void main(String[] args) {
        stringConcat();
    }

    static void stringConcat() {
        String taint = SourceSink.source();
        String s = "abc" + taint + "xyz";
        SourceSink.sink(s); // taint
    }
}
