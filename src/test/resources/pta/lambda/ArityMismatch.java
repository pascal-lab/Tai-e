import java.lang.reflect.Method;
import java.util.function.Supplier;

class ArityMismatch {

    static class App {
        void run() {
            this.get("k");
        }

        Object get(String key) {
            return key;
        }
    }

    public static void main(String[] args) throws Exception {
        Supplier<String> supplier = () -> "x";
        Object target = args.length > 0 ? supplier : new App();
        Method run = App.class.getMethod("run");
        run.invoke(target, new Object[0]);
    }
}
