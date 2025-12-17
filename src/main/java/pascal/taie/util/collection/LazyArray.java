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
 * An abstract class representing an array with lazy initialization semantics.
 * It is designed to efficiently support sparse data by creating elements
 * only when they are first accessed via the {@link #get(int)} method.
 * Subclasses must implement the {@link #createElement()} method to define
 * how elements are constructed.
 *
 * @param <T> the type of the elements held in this array
 */
public abstract class LazyArray<T> {

    private final Object[] elements;

    protected LazyArray(int initialCapacity) {
        elements = new Object[initialCapacity];
    }

    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0 || index >= elements.length) {
            throw new IndexOutOfBoundsException();
        }
        if (elements[index] == null) {
            elements[index] = createElement();
        }
        return (T) elements[index];
    }

    public boolean contains(int index) {
        return index >= 0 && index < elements.length && elements[index] != null;
    }

    protected abstract T createElement();
}
