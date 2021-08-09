class ArgToBase {

    public static void main(String[] args) {
        String taint = SourceSink.source();
        StringBuffer sb = new StringBuffer();
        sb.append("xyz");
        sb.append(taint);
        SourceSink.sink(sb.toString()); // taint
    }
}
