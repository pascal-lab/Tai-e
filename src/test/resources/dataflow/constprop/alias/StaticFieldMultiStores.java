class StaticFieldMultiStores {

    static int f;

    static int g;

    public static void main(String[] args) {
        storeConst();
        storeNAC();
        int x = f;
        int y = g;
    }

    static void storeConst() {
        if (getNAC() > 0) {
            f = 555;
        } else {
            f = 555;
        }
    }

    static void storeNAC() {
        if (getNAC() > 0) {
            g = 666;
        } else {
            g = 777;
        }
    }

    static int getNAC() {
        int i;
        for (i = 0; i < 5; ++i) {
        }
        return i;
    }
}
