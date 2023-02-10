class TaintParam {

    public static void main(String[] args) {
        paramSource(args, args);
    }

    static void paramSource(String[] taint, String[] noTaint) {
        String[] sa1 = taint;
        String[] sa2 = sa1;
        sink(sa2); // taint
        sink(noTaint); // no taint
    }

    static void sink(String[] sa) {
    }
}
