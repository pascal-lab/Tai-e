/**
 * Simple generic class with one type parameter.
 */
public class GenericClass<T> {
    private T value;

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
