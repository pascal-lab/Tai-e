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

/**
 * Similar to {@link java.util.ArrayList}, but optimized for primitive type <code>int</code>
 * Current implementation is quite naive, but is enough for our main purpose: used in frontend
 */
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

