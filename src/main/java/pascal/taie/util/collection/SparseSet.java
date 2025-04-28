/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.util.collection;


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
