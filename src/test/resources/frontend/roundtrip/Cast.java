public class Cast {

    public static void main(String[] args) {
        int i = 257;
        check((byte) i == 1);
        check((short) i == 257);
        check((char) i == 257);

        long l = i;
        float f = l;
        double d = f;
        check(l == 257L);
        check(f == 257.0f);
        check(d == 257.0);

        double x = 3.9;
        check((int) x == 3);
        check((long) x == 3L);

        Object object = new Child(6);
        Parent parent = (Parent) object;
        Child child = (Child) parent;
        check(parent.value() == 6);
        check(child.doubleValue() == 12);

        System.out.println("OK");
    }

    private static class Parent {

        final int value;

        Parent(int value) {
            this.value = value;
        }

        int value() {
            return value;
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
