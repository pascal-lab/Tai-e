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
 * <p>A lazy initialization array.</p>
 * <p>A typical use case is to store sparse data.</p>
 * @param <T> The type of element
 */
public abstract class LazyArray<T> {
    private final Object[] items;

    public LazyArray(int initialCapacity) {
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

