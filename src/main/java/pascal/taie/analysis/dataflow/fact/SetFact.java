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

import pascal.taie.util.Copyable;
import pascal.taie.util.collection.CollectionUtils;
import pascal.taie.util.collection.GenericBitSet;
import pascal.taie.util.collection.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Represents set-like data-flow facts.
 *
 * @param <E> type of elements
 */
public class SetFact<E> implements Copyable<SetFact<E>>, Iterable<E> {

    protected final Set<E> set;

    public SetFact(Collection<E> c) {
        if (c instanceof GenericBitSet<E> s) {
            set = s.copy();
        } else {
            set = Sets.newHybridSet(c);
        }
    }

    public SetFact() {
        this(Collections.emptySet());
    }

    /**
     * @return true if this set contains the specified element, otherwise false.
     */
    public boolean contains(E e) {
        return set.contains(e);
    }

    /**
     * Adds an element to this fact.
     *
     * @return true if this fact changed as a result of the call, otherwise false.
     */
    public boolean add(E e) {
        return set.add(e);
    }

    /**
     * Removes an element from this fact.
     *
     * @return true if an element was removed as a result of the call, otherwise false.
     */
    public boolean remove(E e) {
        return set.remove(e);
    }

    /**
     * Removes all the elements of this fact that satisfy the given predicate.
     *
     * @return true if any elements were removed as a result of the call,
     * otherwise false.
     */
    public boolean removeIf(Predicate<E> filter) {
        return set.removeIf(filter);
    }

    /**
     * Removes all elements of other fact.
     *
     * @return true if this fact changed as a result of the call, otherwise false.
     */
    public boolean removeAll(SetFact<E> other) {
        return set.removeAll(other.set);
    }

    /**
     * Unions other fact into this fact.
     *
     * @return true if this fact changed as a result of the call, otherwise false.
     */
    public boolean union(SetFact<E> other) {
        return set.addAll(other.set);
    }

    /**
     * @return a new fact which is the union of this and other facts.
     */
    public SetFact<E> unionWith(SetFact<E> other) {
        SetFact<E> result = copy();
        result.union(other);
        return result;
    }

    /**
     * Intersects this fact with other fact.
     *
     * @return true if this fact changed as a result of the call, otherwise false.
     */
    public boolean intersect(SetFact<E> other) {
        return set.retainAll(other.set);
    }

    /**
     * @return a new fact which is the intersection of this and other facts.
     */
    public SetFact<E> intersectWith(SetFact<E> other) {
        SetFact<E> result = copy();
        result.intersect(other);
        return result;
    }

    /**
     * Sets the content of this set to the same as other set.
     */
    public void set(SetFact<E> other) {
        if (set instanceof GenericBitSet<E> s) {
            s.setTo(other.set);
        } else {
            clear();
            union(other);
        }
    }

    /**
     * Creates and returns a copy of this fact.
     */
    @Override
    public SetFact<E> copy() {
        return new SetFact<>(this.set);
    }

    /**
     * Clears all content in this fact.
     */
    public void clear() {
        set.clear();
    }

    public boolean isEmpty() {
        return set.isEmpty();
    }

    public Stream<E> stream() {
        return set.stream();
    }

    @Override
    public Iterator<E> iterator() {
        return set.iterator();
    }

    public void forEach(Consumer<? super E> action) {
        set.forEach(action);
    }

    public int size() {
        return set.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SetFact<?> that)) {
            return false;
        }
        return set.equals(that.set);
    }

    @Override
    public int hashCode() {
        return set.hashCode();
    }

    @Override
    public String toString() {
        return CollectionUtils.toString(set);
    }
}
