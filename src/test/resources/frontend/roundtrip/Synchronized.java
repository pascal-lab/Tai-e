public class Synchronized {

    private int value;

    public static void main(String[] args) {
        Synchronized object = new Synchronized();

        object.increment();
        check(object.get() == 1);

        synchronized (object) {
            object.value += 2;
        }
        check(object.get() == 3);

        System.out.println("OK");
    }

    private synchronized void increment() {
        ++value;
    }

    private int get() {
        return value;
    }

    private static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
