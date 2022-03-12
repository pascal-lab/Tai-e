class Literal {
    public static int f() {
        int a = 1 + 1 + 1;
        return 1 + 1 + 1;
    }

    public static double g() {
        return 1.0 + 100000000;
    }

    public static Object h() {
        return null;
    }

    public static String s(String h) {
        return h + "123" + 123 + 123.4 + new Literal();
    }

    public static double h1() {
        return 1;
    }
}