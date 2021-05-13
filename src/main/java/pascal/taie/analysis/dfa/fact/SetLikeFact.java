/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.analysis.dfa.fact;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import static pascal.taie.util.collection.CollectionUtils.newHybridSet;

/**
 * TODO: implement copy-on-write?
 * @param <E> type of elements
 */
public class SetLikeFact<E> {

    private final Set<E> set;

    private SetLikeFact(Collection<E> c) {
        set = newHybridSet(c);
    }

    /**
     * Creates a set-like fact containing the elements in the given collection.
     */
    public static <T> SetLikeFact<T> make(Collection<T> c) {
        return new SetLikeFact<>(c);
    }

    /**
     * Creates a empty set-like fact.
     */
    public static <T> SetLikeFact<T> make() {
        return new SetLikeFact<>(Collections.emptySet());
    }

    /**
     * Adds an element to this fact.
     * @return if this operation changes this fact.
     */
    public boolean add(E e) {
        return set.add(e);
    }

    /**
     * Removes an element from this fact (if present).
     * @return if this operation changes this fact.
     */
    public boolean remove(E e) {
        return set.remove(e);
    }

    /**
     * Unions other fact.
     * @return if this operation changes this fact.
     */
    public boolean union(SetLikeFact<E> other) {
        return set.addAll(other.set);
    }

    /**
     * Intersects other fact
     * @return if this operation changes this fact.
     */
    public boolean intersect(SetLikeFact<E> other) {
        return set.retainAll(other.set);
    }

    /**
     * Sets the content of this set to other set.
     * @return if this operation changes this fact.
     */
    public boolean setTo(SetLikeFact<E> other) {
        return intersect(other) || union(other);
    }

    /**
     * Creates a duplication of this fact.
     */
    public SetLikeFact<E> duplicate() {
        return make(this.set);
    }

    /**
     * Clears all content in this fact.
     */
    public void clear() {
        set.clear();
    }

    public Stream<E> stream() {
        return set.stream();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SetLikeFact<?> that = (SetLikeFact<?>) o;
        return set.equals(that.set);
    }

    @Override
    public int hashCode() {
        return set.hashCode();
    }

    @Override
    public String toString() {
        return set.toString();
    }
}
