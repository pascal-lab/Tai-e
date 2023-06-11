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

import java.io.Serializable;
import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A Queue implementation which contains no duplicate elements.
 *
 * @param <E> type of elements.
 */
public class SetQueue<E> extends AbstractQueue<E> implements Serializable {

    private final Set<E> set = new LinkedHashSet<>();

    @Override
    public Iterator<E> iterator() {
        return set.iterator();
    }

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public boolean add(E e) {
        return set.add(e);
    }

    @Override
    public boolean offer(E e) {
        return set.add(e);
    }

    @Override
    public E poll() {
        Iterator<E> it = set.iterator();
        if (it.hasNext()) {
            E e = it.next();
            it.remove();
            return e;
        } else {
            return null;
        }
    }

    @Override
    public E peek() {
        Iterator<E> it = set.iterator();
        return it.hasNext() ? it.next() : null;
    }
}
