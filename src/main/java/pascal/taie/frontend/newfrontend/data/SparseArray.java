package pascal.taie.frontend.newfrontend.data;

public abstract class SparseArray<T> {
    private final Object[] items;

    public SparseArray(int initialCapacity) {
        items = new Object[initialCapacity];
    }

    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0 || index >= items.length) {
            throw new IndexOutOfBoundsException();
        }
        if (items[index] == null) {
            items[index] = createInstance();
        }
        return (T) items[index];
    }

    public boolean has(int index) {
        return index >= 0 && index < items.length && items[index] != null;
    }

    protected abstract T createInstance();

    public Object[] getItems() {
        return items;
    }
}

