class ArrayTaint {
    public static void main(String[] args) {
        String[] arr = new String[10];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = SourceSink.source();
        }
        SourceSink.sink(join(",", arr));
    }

    public static String join(String delim, String[] strings) {]
        if (strings == null || strings.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strings.length - 1; i++) {
            sb.append(strings[i]);
            sb.append(delim);
        }
        sb.append(strings[strings.length - 1]);
        return sb.toString();
    }
}