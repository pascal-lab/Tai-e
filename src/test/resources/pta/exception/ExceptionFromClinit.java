public class ExceptionFromClinit {
    public static void main(String[] args) {
        InstanceThrower it = new InstanceThrower();
        it.throwE();
        throwE();
    }
    public static void throwE() {
        Object.class.toString();
        throw new RuntimeException();
    }
}
class InstanceThrower {
    public void throwE() {
        StaticThrower.throwE();
    }
}
class StaticThrower {
    static {
        throwE();
    }
    public static void throwE() {
        ExceptionFromClinit.throwE();
    }
}
