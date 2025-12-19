import java.util.AbstractList;

/**
 * Generic class extending another generic class.
 */
public class ExtendingGenericClass<E> extends AbstractList<E> {
    private Object[] elements;
    private int size;

    @Override
    public E get(int index) {
        return (E) elements[index];
    }

    @Override
    public int size() {
        return size;
    }
}
