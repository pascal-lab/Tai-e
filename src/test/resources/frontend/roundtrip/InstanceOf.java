public class InstanceOf {

    public static void main(String[] args) {
        Object child = new Child(5);
        Object parent = new Parent(3);
        Object text = "text";
        Object none = null;

        check(child instanceof Parent);
        check(child instanceof Child);
        check(parent instanceof Parent);
        check(!(parent instanceof Child));
        check(text instanceof String);
        check(!(none instanceof Parent));

        if (child instanceof Child c) {
            check(c.doubleValue() == 10);
        } else {
            throw new AssertionError();
        }

        System.out.println("OK");
    }

    private static class Parent {

        final int value;

        Parent(int value) {
            this.value = value;
        }
    }

    private static class Child extends Parent {

        Child(int value) {
            super(value);
        }

        int doubleValue() {
            return value * 2;
        }
    }

    private static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
