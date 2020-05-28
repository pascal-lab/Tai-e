public class InstanceField2 {

    public static void main(String[] args) {
        entry1();
        entry2();
    }

    private Object field;


    private void f() {
        field = new Object();
        g();
    }

    private void g() {
        Object local = field;
    }

    public static void entry1() {
        new InstanceField2().f();
    }

    public static void entry2() {
        new InstanceField2().f();
    }
}
