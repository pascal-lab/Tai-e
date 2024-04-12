package pascal.taie.frontend.newfrontend;


import pascal.taie.frontend.newfrontend.data.IntList;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

/**
 * <p>Sparse Set, don't confuse with {@link pascal.taie.util.collection.SparseBitSet}</p>
 * <p>This data structure will be more efficient (O(k), k is the element size) for element iterate</p>
 */
public class SparseSet implements Iterable<Integer> {
    private final BitSet bitSet;

    private final IntList list;

    public SparseSet(int maxV, int cap) {
        bitSet = new BitSet(maxV + 1);
        list = new IntList(4);
    }

    public boolean has(int x) {
        return bitSet.get(x);
    }

    public void add(int x) {
        if (has(x)) {
            return;
        }
        bitSet.set(x);
        list.add(x);
    }

    public boolean isEmpty() {
        return list.size() == 0;
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public int removeLast() {
        throw new UnsupportedOperationException();
    }

    public void delete(int x) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Nonnull
    public Iterator<Integer> iterator() {
        return new Iterator<>() {
            int index = 0;

            @Override
            public boolean hasNext() {
                return index < list.size();
            }

            @Override
            public Integer next() {
                return list.get(index++);
            }
        };
    }

    public List<Integer> toList() {
        List<Integer> res = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); ++i) {
            res.add(list.get(i));
        }
        return res;
    }

    public int size() {
        return list.size();
    }

}
