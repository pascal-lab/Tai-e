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

package pascal.taie.analysis.dataflow.fact;

import pascal.taie.util.collection.SetUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents set-like data-flow facts.
 * TODO: implement copy-on-write?
 * @param <E> type of elements
 */
public class SetFact<E> {

    protected final Set<E> set;

    public SetFact(Collection<E> c) {
        set = SetUtils.newHybridSet(c);
    }

    public SetFact() {
        this(Collections.emptySet());
    }

    /**
     * @return true if this set contains the specified element.
     */
    public boolean contains(Object e) {
        //noinspection SuspiciousMethodCalls
        return set.contains(e);
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
     * Removes all of the elements of this collection that satisfy the given predicate.
     * @return if this operation changes this fact.
     */
    public boolean removeIf(Predicate<E> filter) {
        return set.removeIf(filter);
    }

    /**
     * Unions other fact.
     * @return if this operation changes this fact.
     */
    public boolean union(SetFact<E> other) {
        return set.addAll(other.set);
    }

    /**
     * @return a new fact which is the union of given f1 and f2.
     */
    public SetFact<E> unionWith(SetFact<E> other) {
        SetFact<E> result = duplicate();
        result.union(other);
        return result;
    }

    /**
     * Intersects other fact
     * @return if this operation changes this fact.
     */
    public boolean intersect(SetFact<E> other) {
        return set.retainAll(other.set);
    }

    /**
     * @return a new fact which is the intersection of this and other.
     */
    public SetFact<E> intersectWith(SetFact<E> other) {
        SetFact<E> result = duplicate();
        result.intersect(other);
        return result;
    }

    /**
     * Sets the content of this set to other set.
     */
    public void set(SetFact<E> other) {
        clear();
        union(other);
    }

    /**
     * Creates a duplication of this fact.
     */
    public SetFact<E> duplicate() {
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

    public int size() {
        return set.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SetFact)) {
            return false;
        }
        SetFact<?> that = (SetFact<?>) o;
        return set.equals(that.set);
    }

    @Override
    public int hashCode() {
        return set.hashCode();
    }

    @Override
    public String toString() {
        // Sort elements by their string representation, so that the fact
        // representation is stable across executions. This is useful for
        // comparing expected results and the ones given by the analysis.
        return "[" + set.stream()
                .map(Object::toString)
                .sorted()
                .collect(Collectors.joining(", ")) + "]";
    }
}
