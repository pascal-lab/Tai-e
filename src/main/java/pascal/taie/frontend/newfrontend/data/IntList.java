package pascal.taie.frontend.newfrontend.data;

public class IntList {
    private int[] items;
    private int size;

    public IntList(int initialCapacity) {
        items = new int[initialCapacity];
        size = 0;
    }

    public void add(int item) {
        ensureCapacity();
        items[size] = item;
        size++;
    }

    public int get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException();
        }
        return items[index];
    }

    public int size() {
        return size;
    }

    public int[] getItems() {
        return items;
    }

    private void ensureCapacity() {
        if (size == items.length) {
            int[] newItems = new int[items.length * 2];
            System.arraycopy(items, 0, newItems, 0, items.length);
            items = newItems;
        }
    }
}

