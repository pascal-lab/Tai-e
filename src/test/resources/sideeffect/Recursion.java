public class Recursion {
    public static void main(String[] args) {
        recur1(0);
    }

    static void recur1(int n) {
        recur2(n);
        A a = new A();
        a.name = "recur1";
    }

    static void recur2(int n) {
        A a = new A();
        a.name = "recur2";
        recur3(n);
        leaf1();
    }

    static void recur3(int n) {
        if (n < 10) {
            recur1(n + 1);
        } else {
            leaf2();
        }
    }

    static void leaf1() {
        String[] sa = new String[]{"leaf1"};
    }

    static void leaf2() {
        String[] sa = new String[]{"leaf2"};
        leaf3();
    }

    static void leaf3() {
        String[] sa = new String[]{"leaf3"};
    }
}

class A {
    String name;
}
