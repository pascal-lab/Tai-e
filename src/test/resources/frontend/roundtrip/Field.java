public class Field {

    private static int counter = 1;

    private final int value;

    private final Nested nested;

    public Field(int value) {
        this.value = value;
        this.nested = new Nested(value + 1);
    }

    public static void main(String[] args) {
        Field field = new Field(10);

        check(counter == 1);
        counter += 2;
        check(counter == 3);

        check(field.value == 10);
        check(field.nested.get() == 11);

        System.out.println("OK");
    }

    private static class Nested {

        private final int value;

        Nested(int value) {
            this.value = value;
        }

        int get() {
            return value;
        }
    }

    private static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
