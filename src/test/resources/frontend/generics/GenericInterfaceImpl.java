/**
 * Implementation of generic interface.
 */
public class GenericInterfaceImpl<T> implements GenericInterface<T> {
    private T value;

    @Override
    public T get() {
        return value;
    }

    @Override
    public void set(T value) {
        this.value = value;
    }
}
