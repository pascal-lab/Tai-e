/**
 * Class with recursive type bound (F-bounded polymorphism).
 */
public class RecursiveBoundClass<T extends RecursiveBoundClass<T>> {
    private T self;

    public T getSelf() {
        return self;
    }

    public int compareWith(T other) {
        return 0;
    }
}
