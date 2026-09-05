public class LineNumber {
    static void use(Object value) {
    }

    static void ssa(String[] args) {
        boolean flag = args.length == 0;
        Object value = flag ? new Object() : new Object();
        use(value);
    }

    static void loop(int count) {
        int value = 0;
        while (count > 0) {
            value = value + 1;
            count--;
        }
        use(value);
    }

    public static void main(String[] args) {
        use(args.length == 0 ? args : null);
    }

    static void emptyConditional(boolean flag) {
        use(flag
                ? new Object()
                : null);
    }

    static void synchronizedCase(Object value) {
        synchronized (value) {
            use(value);
        }
        synchronized (value) {
            use(value);
        }
    }

    static void splitStore(boolean flag, String text) {
        Object value;
        if (flag) {
            value = new StringBuilder(
                    text.length());
        } else {
            value = new String(text);
        }
        use(value);
    }

    static <T extends Object & Comparable<? super T>> int preciseCompare(
            java.util.Set<T> first, java.util.Set<T> second) {
        T[] firstArray = (T[]) first.toArray();
        T[] secondArray = (T[]) second.toArray();
        java.util.Arrays.sort(firstArray);
        java.util.Arrays.sort(secondArray);
        return java.util.Arrays.compare(firstArray, secondArray);
    }
}
