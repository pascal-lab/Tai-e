public class String {

    public static void main(java.lang.String[] args) {
        java.lang.String a = "Tai";
        java.lang.String b = "-e";
        java.lang.String c = a + b;

        check(c.equals("Tai-e"));
        check(c.length() == 5);
        check(c.substring(0, 3).equals("Tai"));
        check(java.lang.String.valueOf(123).equals("123"));

        java.lang.StringBuilder builder = new java.lang.StringBuilder();
        builder.append(a);
        builder.append(b);
        check(builder.toString().equals("Tai-e"));

        System.out.println("OK");
    }

    private static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
