class StringAppend {

    public static void main(String[] args) {
        stringAdd();
        stringBuffer();
        stringBuilder();
    }

    static void stringAdd() {
        String taint = SourceSink.source();
        String s = "abc" + taint + "xyz";
        SourceSink.sink(s); // taint
    }

    static void stringBuffer() {
        String taint = SourceSink.source();
        StringBuffer sb = new StringBuffer();
        sb.append("abc");
        sb.append(taint);
        sb.append("xyz");
        String s = sb.toString();
        SourceSink.sink(s); // taint
    }

    static void stringBuilder() {
        String taint = SourceSink.source();
        StringBuilder sb = new StringBuilder();
        sb.append("abc");
        sb.append(taint);
        sb.append("xyz");
        String s = sb.toString();
        SourceSink.sink(s); // taint
    }
}
