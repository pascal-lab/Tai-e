public class Literal {

    public static void main(String[] args) {
        check(true);
        check('\n' == 10);
        check(123 == 123);
        check(123L == 123L);
        check(1.5f == 1.5f);
        check(2.5 == 2.5);

        Object nullValue = null;
        check(nullValue == null);

        check(Literal.class.getName().equals("Literal"));
        check(int.class.getName().equals("int"));
        check(String[].class.isArray());

        System.out.println("OK");
    }

    private static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
