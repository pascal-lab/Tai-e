class Strings {

    public static void main(String[] args) {
        String s1 = id("aaa");

        A a = new A();
        a.f = "bbb";
        String s2 = a.f;

        A.sf = "ccc";
        String s3 = A.sf;

        String[] arr = new String[10];
        arr[0] = "ddd";
        String s4 = arr[0];
    }

    static String id(String s) {
        return s;
    }
}

class A {
    static String sf;
    String f;
}
