class StaticTaintTransfer {

    public static void main(String[] args) {
        String arr[] = new String[1];
        arr[0] = SourceSink.source();
        String result[] = mapString(arr, new Mapper() {
            @Override
            public String map(String s) {
                return concat("hello, ", s);
            }
        });
        SourceSink.sink(result[0]);
    }

    static String concat(String lhs, String rhs) {
        return new String();
    }

    static String[] mapString(String arr[], Mapper mapper) {
        String result[] = new String[arr.length];
        for (int i = 0; i < arr.length; ++i) {
            result[i] = mapper.map(arr[i]);
        }
        return result;
    }
}

interface Mapper {
    public String map(String s);
}