package pascal.taie.frontend.newfrontend;


import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <p>Sparse Set, don't confuse with {@link pascal.taie.util.collection.SparseBitSet}</p>
 * <p>This data structure will be more efficient (O(k), k is the element size) for element iterate</p>
 */
public class SparseSet implements Iterable<Integer> {
    private final int[] sparse;
    private final int[] dense;
    private final int capacity;
    private final int maxValue;
    private int numberOfElements;

    public static int NOT_EXIST = -1;

    public SparseSet(int maxV, int cap) {
        sparse = new int[maxV + 1];
        dense = new int[cap];
        capacity = cap;
        maxValue = maxV;
        numberOfElements = 0;
    }

    public int search(int x) {

        if (x > maxValue) {
            return NOT_EXIST;
        }

        if (sparse[x] < numberOfElements && dense[sparse[x]] == x) {
            return sparse[x];
        }
        return NOT_EXIST;
    }

    public boolean has(int x) {
        return search(x) != NOT_EXIST;
    }

    public void add(int x) {

        assert !(x > maxValue || numberOfElements >= capacity);
        // the element already exists in the set
        if (search(x) != NOT_EXIST) {
            return;
        }

        // add the element to the end of the dense array
        dense[numberOfElements] = x;

        // update the index of the element in the sparse array
        sparse[x] = numberOfElements;

        numberOfElements++; // increment the size of the set
    }

    public void union(SparseSet another) {
        for (int i = 0; i < another.numberOfElements; ++i) {
            add(another.dense[i]);
        }
    }

    public boolean isEmpty() {
        return numberOfElements == 0;
    }

    public void clear() {
        numberOfElements = 0;
    }

    public int removeLast() {
        assert numberOfElements > 0;
        int res = dense[numberOfElements - 1];
        numberOfElements--;
        return res;
    }

    public void delete(int x) {

        int index = search(x); // find the index of the element

        // check if the element exists in the set
        if (index == NOT_EXIST) {
            return; // if not, do nothing and return
        }

        // swap the element with the last element in the dense array
        int temp = dense[numberOfElements - 1];
        dense[index] = temp;
        sparse[temp] = index;
        numberOfElements--; // decrement the size of the set
    }

    @Override
    @Nonnull
    public Iterator<Integer> iterator() {
        return new Iterator<>() {
            int index = 0;

            @Override
            public boolean hasNext() {
                return index < numberOfElements;
            }

            @Override
            public Integer next() {
                return dense[index++];
            }
        };
    }

    public List<Integer> toList() {
        List<Integer> res = new ArrayList<>(numberOfElements);
        for (int i = 0; i < numberOfElements; ++i) {
            res.add(dense[i]);
        }
        res.sort(Integer::compareTo);
        return res;
    }

    public int size() {
        return numberOfElements;
    }

}
