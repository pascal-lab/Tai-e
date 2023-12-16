class TaintCall {
    public static void main(String[] args) {
        String s = new String();
        varArg(s);
        sink(s); // taint

        arrayArg(args);
        sink(args[0]); // taint

        A a = new A();
        fieldArg(a);
        sink(a.f); // taint

        String[] cmds = source();
        String cmd = cmds[0];
        sink(cmd); // taint
    }

    static void varArg(String arg) {
    }

    static void arrayArg(String[] args) {
    }

    static void fieldArg(A a) {
    }

    static String[] source() {
        return new String[]{"taint"};
    }

    static void sink(String s) {
    }
}

class A {
    String f;
}
