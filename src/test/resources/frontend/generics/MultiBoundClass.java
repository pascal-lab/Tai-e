import java.io.Serializable;

/**
 * Generic class with multiple bounds (class and interface).
 */
public class MultiBoundClass<T extends Number & Comparable<T> & Serializable> {
    private T value;

    public T getValue() {
        return value;
    }
}
