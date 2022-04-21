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

package pascal.taie.analysis.dataflow.fact;

import pascal.taie.util.Hashes;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Represents set-like data-flow facts.
 * This fact can represent a top element in the lattice, i.e., Universe.
 * Note that the top element is conceptual, i.e., it is mock and does not
 * really contain all elements in the domain, thus remove and iteration
 * operations on the top element are unsupported.
 *
 * @param <E> type of elements
 */
public class ToppedSetFact<E> extends SetFact<E> {

    private boolean isTop;

    public ToppedSetFact(boolean isTop) {
        this.isTop = isTop;
    }

    public ToppedSetFact(Collection<E> c) {
        super(c);
        this.isTop = false;
    }

    public boolean isTop() {
        return isTop;
    }

    public void setTop(boolean top) {
        isTop = top;
        if (isTop) {
            // top element is mock and does not need to store any values.
            set.clear();
        }
    }

    @Override
    public boolean contains(E e) {
        return isTop || super.contains(e);
    }

    @Override
    public boolean add(E e) {
        return !isTop && super.add(e);
    }

    @Override
    public boolean remove(E e) {
        if (isTop) {
            throw new UnsupportedOperationException();
        }
        return super.remove(e);
    }

    @Override
    public boolean removeIf(Predicate<E> filter) {
        if (isTop) {
            throw new UnsupportedOperationException();
        }
        return super.removeIf(filter);
    }

    @Override
    public boolean union(SetFact<E> other) {
        if (isTop) {
            return false;
        }
        ToppedSetFact<E> fact = (ToppedSetFact<E>) other;
        if (fact.isTop) {
            setTop(true);
            return true;
        }
        return super.union(other);
    }

    @Override
    public boolean intersect(SetFact<E> other) {
        ToppedSetFact<E> fact = (ToppedSetFact<E>) other;
        if (fact.isTop) {
            return false;
        }
        if (isTop) {
            set(other);
            return true;
        }
        return super.intersect(other);
    }

    @Override
    public void set(SetFact<E> other) {
        ToppedSetFact<E> fact = (ToppedSetFact<E>) other;
        isTop = fact.isTop;
        super.set(other);
    }

    @Override
    public ToppedSetFact<E> copy() {
        ToppedSetFact<E> copy = new ToppedSetFact<>(set);
        copy.setTop(isTop);
        return copy;
    }

    @Override
    public void clear() {
        isTop = false;
        super.clear();
    }

    @Override
    public boolean isEmpty() {
        return !isTop && super.isEmpty();
    }

    @Override
    public Stream<E> stream() {
        if (isTop) {
            throw new UnsupportedOperationException();
        }
        return super.stream();
    }

    @Override
    public int size() {
        if (isTop) {
            throw new UnsupportedOperationException();
        }
        return super.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ToppedSetFact<?> that = (ToppedSetFact<?>) o;
        return isTop == that.isTop && super.equals(that);
    }

    @Override
    public int hashCode() {
        return Hashes.hash(isTop, super.hashCode());
    }

    @Override
    public String toString() {
        return isTop ? "{TOP}" : super.toString();
    }
}
