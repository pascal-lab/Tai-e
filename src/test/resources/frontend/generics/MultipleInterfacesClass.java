import java.io.Serializable;

/**
 * Generic class implementing multiple generic interfaces.
 */
public class MultipleInterfacesClass<T> implements Comparable<T>, Serializable {
    private static final long serialVersionUID = 1L;

    private T value;

    @Override
    public int compareTo(T o) {
        return 0;
    }
}
