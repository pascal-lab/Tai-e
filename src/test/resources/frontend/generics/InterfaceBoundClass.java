/**
 * Generic class with interface bound.
 */
public class InterfaceBoundClass<T extends Comparable<T>> {
    private T value;

    public int compareTo(T other) {
        return value.compareTo(other);
    }

    public T getValue() {
        return value;
    }
}
