class TaintCorner {
    public static void main(String args[]) {
        corner1();
        corner2();
    }


    public static void corner1() {//taint obj flow to wrong sink index
        String taint = SourceSink.source();
        String s = new String();
        SourceSink.sink(taint, s);
    }

    public static void corner2() {//source&sink in one method
        String s1 = SourceSink.source();
        String s2 = new String();
        String taint = SourceSink.sourceAndSink(s1, s2);
        SourceSink.sink(taint);
    }

}