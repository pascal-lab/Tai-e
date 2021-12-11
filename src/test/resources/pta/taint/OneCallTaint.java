class OneCallTaint {
    public static void main(String args[]) {
        String s1 = new String(); //s1 safe
        String s2 = SourceSink.source(); //s2 not safe

        String ss1 = identity(s1); //ss1 safe, but ci mix s2 in ss1.
        String ss2 = identity(s2);// ss2 not safe

        SourceSink.sink(ss1);

    }

    static String identity(String s) {
        return s;
    }

}