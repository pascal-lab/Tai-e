class TaintParam {
    public static void main(String[] args) {
        varParam(args, new String[]{"noTaint"});
        arrayParam(new String[]{"taint"});
        fieldParam(new A());
    }

    static void varParam(String[] taint, String[] noTaint) {
        String[] sa1 = taint;
        String[] sa2 = sa1;
        sink(sa2); // taint
        sink(noTaint); // no taint
    }

    static void arrayParam(String[] taints) {
        sink(taints[0]); // taint
        sink(taints); // taint
    }

    static void fieldParam(A a1) {
        String taint = a1.f;
        sink(taint); // taint
        sink(a1); // taint
    }

    static void sink(String[] sa) {
    }

    static void sink(String sa) {
    }

    static void sink(A a) {
    }
}

class A {
    String f;
}
