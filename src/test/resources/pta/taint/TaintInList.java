class TaintInList {

    public static void main(String[] args) {
        StringList l1 = new StringList();
        l1.add(SourceSink.source());
        String s1 = l1.get(0);
        SourceSink.sink(s1);

        StringList l2 = new StringList();
        l2.add(new String());
        String s2 = l2.get(0);
        SourceSink.sink(s2);
    }
}

class StringList {

    private String[] elements = new String[10];

    private int size = 0;

    void add(String s) {
        ensureCapacity(size + 1);
        elements[size++] = s;
    }

    private void ensureCapacity(int capacity) {
        if (capacity > elements.length) {
            String[] tmp = new String[elements.length * 2];
            for (int i = 0; i < elements.length) {
                tmp[i] = elements[i];
            }
            elements = tmp;
        }
    }

    String get(int i) {
        return elements[i];
    }
}
