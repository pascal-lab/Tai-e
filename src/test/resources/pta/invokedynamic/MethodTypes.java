import java.lang.invoke.MethodType;

public class MethodTypes {

    public static void main(String[] args) {
        MethodType mt0arg = MethodType.methodType(String.class);
        MethodType mt1arg = MethodType.methodType(String.class, Object.class);
        MethodType mtmt = MethodType.methodType(Object.class, mt1arg);
        use(mt0arg, mt1arg, mtmt);
    }

    static void use(Object... objects) {
    }
}
