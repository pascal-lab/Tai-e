class LazyHeapModeling {

    public static void main(String[] args) throws Exception {
        String name = args[0];
        Class c = Class.forName(name);
        Object o = c.newInstance();
        castToV(o);
    }

    private static void castToV(Object o) {
        V v = (V) o;
        v.toString();
    }
}
