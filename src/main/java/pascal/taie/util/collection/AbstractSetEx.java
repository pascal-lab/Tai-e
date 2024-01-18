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

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public abstract class AbstractSetEx<E> extends AbstractSet<E>
        implements SetEx<E> {

    @Override
    public SetEx<E> copy() {
        SetEx<E> copy = newSet();
        copy.addAll(this);
        return copy;
    }

    @Override
    public SetEx<E> addAllDiff(Collection<? extends E> c) {
        SetEx<E> diff = newSet();
        for (E e : c) {
            if (add(e)) {
                diff.add(e);
            }
        }
        return diff;
    }

    /**
     * Creates and returns a new set. The type of the new set should be the
     * corresponding subclass.
     * This method is provided to ease the implementation of {@link #copy()}
     * and {@link #addAllDiff(Collection)}. If a subclass overwrites
     * above two methods, it does not need to re-implement this method.
     */
    protected SetEx<E> newSet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasOverlapWith(Set<E> other) {
        return !Collections.disjoint(this, other);
    }
}
