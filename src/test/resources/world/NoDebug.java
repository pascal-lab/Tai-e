public class NoDebug {
    static void use(Object value) {
        System.out.println(value);
    }

    public static void main(String[] args) {
        use(new Object());
    }
}
