class BackPropagation {

    private char[] buffer = new char[100];

    private StringBuilder sb = new StringBuilder();

    public static void main(String[] args) {
        BackPropagation bp = new BackPropagation();
        bp.taintCharArrayVar();
        bp.taintCharArrayField();
        bp.taintStringBuilderVar();
        bp.taintStringBuilderField();
    }

    void taintCharArrayVar() {
        String s1 = SourceSink.source();
        char[] chars = new char[100];
        s1.getChars(0, s1.length(), chars, 0);
        String s2 = new String(chars);
        SourceSink.sink(s2); // taint
    }

    void taintCharArrayField() {
        String s1 = SourceSink.source();
        s1.getChars(0, s1.length(), buffer, 0);
        String s2 = new String(buffer);
        SourceSink.sink(s2); // taint
    }

    void taintStringBuilderVar() {
        String s1 = SourceSink.source();
        StringBuilder builder = new StringBuilder();
        builder.append(s1);
        String s2 = builder.toString();
        SourceSink.sink(s2); // taint
    }

    void taintStringBuilderField() {
        String s1 = SourceSink.source();
        sb.append(s1);
        String s2 = sb.toString();
        SourceSink.sink(s2); // taint
    }
}
