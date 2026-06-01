import java.util.List;

/**
 * Class with generic array types.
 */
public class GenericArrayClass<T> {
    private T[] array;
    private List<T>[] listArray;
    private T[][] twoDArray;

    public T[] getArray() {
        return array;
    }

    public <E> E[] toArray(E[] a) {
        return a;
    }
}
