class LazyHeapModeling {

    public static void main(String[] args) throws Exception {
        String name = args[0];
        Class c = Class.forName(name);
        Object o = c.newInstance();
        PTAAssert.calls("<U: void <init>()>", "<V: void <init>()>");
        castToV(o);
    }

    private static void castToV(Object o) {
        V v = (V) o;
        PTAAssert.hasInstanceOf(v, "U", "V");
    }
}
