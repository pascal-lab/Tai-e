class InstanceSourceSink {

    public static void main(String args[]) {
        InstanceSourceSink source = new InstanceSourceSink();
        String taint = source.instanceSource();
        InstanceSourceSink sink = new InstanceSourceSink();
        sink.instanceSink(taint); // taint

        new ProcessBuilder(args).start(); // taint

        Taint t1 = new Taint();
        sink.instanceSink(t1);
        Taint t2 = t1;
        t2.becomeTainted();
        sink.instanceSink(t2); // taint

        Taint t3 = t1;
        t2.becomeTainted(t3);
        sink.instanceSink(t3); // taint
    }

    public String instanceSource() {
        return new String();
    }

    public void instanceSink(String s) {
    }

    public void instanceSink(Taint t) {
    }
}

class ProcessBuilder {

    private String[] cmd;

    ProcessBuilder(String[] cmd) {
        this.cmd = cmd;
    }

    public void start() {
    }
}

class Taint {

    void becomeTainted() {
    }

    void becomeTainted(Taint t) {
    }
}
