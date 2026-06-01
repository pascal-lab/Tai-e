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
import java.util.BitSet;
import java.util.Iterator;

/**
 * A sparse set data structure for storing non-negative integers.
 * <p>
 * This implementation combines a {@link BitSet} for O(1) membership testing
 * with an {@link IntList} that maintains insertion order, enabling O(k)
 * iteration where k is the number of elements in the set (rather than the
 * maximum possible value).
 * <p>
 * <b>Time complexities:</b>
 * <ul>
 *   <li>{@link #add(int)} - O(1)</li>
 *   <li>{@link #contains(int)} - O(1)</li>
 *   <li>Iteration - O(k), where k is the number of elements</li>
 * </ul>
 * <p>
 * <b>Note:</b> Do not confuse with {@link SparseBitSet}, which is a different
 * data structure optimized for sparse bit vectors.
 */
public class SparseIntSet implements Iterable<Integer> {

    private final BitSet membership;

    private final IntList elements;

    /**
     * Creates a new sparse set.
     *
     * @param maxValue the maximum value that can be stored in this set
     */
    public SparseIntSet(int maxValue) {
        membership = new BitSet(maxValue + 1);
        elements = new IntList(4);
    }

    /**
     * Checks if this set contains the specified value.
     */
    public boolean contains(int value) {
        return membership.get(value);
    }

    /**
     * Adds the specified value to this set if not already present.
     *
     * @param value the value to add
     */
    public void add(int value) {
        if (!contains(value)) {
            membership.set(value);
            elements.add(value);
        }
    }

    /**
     * Returns the element at the specified index in insertion order.
     */
    public int get(int index) {
        return elements.get(index);
    }

    /**
     * Returns the number of elements in this set.
     */
    public int size() {
        return elements.size();
    }

    /**
     * Returns {@code true} if this set contains no elements.
     */
    public boolean isEmpty() {
        return elements.size() == 0;
    }

    @Override
    @Nonnull
    public Iterator<Integer> iterator() {
        return new Iterator<>() {
            int index = 0;

            @Override
            public boolean hasNext() {
                return index < elements.size();
            }

            @Override
            public Integer next() {
                return elements.get(index++);
            }
        };
    }

    public int[] toArray() {
        int[] array = new int[elements.size()];
        for (int i = 0; i < elements.size(); i++) {
            array[i] = elements.get(i);
        }
        return array;
    }
}
