class ArrayLoops {

    public static void main(String[] args) {
        loopConst();
        loopMix();
        loopNAC();
    }

    static void loopConst() {
        int[] a = new int[5];
        for (int i = 0; i < a.length; ++i) {
            a[i] = 666;
        }
        int x = a[3];
    }

    static void loopMix() {
        int[] a = new int[5];
        for (int i = 0; i < a.length; ++i) {
            a[i] = 666;
        }
        a[4] = 777;
        int x = a[3];
        int y = a[4];
    }

    static void loopNAC() {
        int[] a = new int[5];
        for (int i = 0; i < a.length; ++i) {
            a[i] = i;
        }
        int x = a[3];
    }
}
