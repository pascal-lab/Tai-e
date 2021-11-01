public class InstanceField2 {

    private Object field;

    public static void main(String[] args) {
        entry1();
        entry2();
    }

    public static void entry1() {
        new InstanceField2().f();
    }

    public static void entry2() {
        new InstanceField2().f();
    }

    private void f() {
        field = new Object();
        g();
    }

    private void g() {
        Object local = field;
    }
}
