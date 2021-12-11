class InstanceSourceSink {

    public static void main(String args[]) {
        InstanceSourceSink source = new InstanceSourceSink();
        String taint = source.instanceSource();
        InstanceSourceSink sink = new InstanceSourceSink();
        sink.instanceSink(taint);
    }

    public String instanceSource() {
        return new String();
    }

    public void instanceSink(String s) {

    }

}
