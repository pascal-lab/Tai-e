class StaticCall {

    public static void main(String[] args) {
        Object o = foo(100, new Object());
    }

    static Object foo(int n, Object o) {
        if (n < 0) {
            return bar(n, o);
        }
        return o;
    }

    static Object bar(int n, Object o) {
        return foo(n--, o);
    }
}
