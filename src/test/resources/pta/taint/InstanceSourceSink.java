class InstanceSourceSink {

    public static void main(String args[]) {
        InstanceSourceSink source = new InstanceSourceSink();
        String taint = source.instanceSource();
        InstanceSourceSink sink = new InstanceSourceSink();
        sink.instanceSink(taint); // taint

        new ProcessBuilder(args).start();
    }

    public String instanceSource() {
        return new String();
    }

    public void instanceSink(String s) {
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
