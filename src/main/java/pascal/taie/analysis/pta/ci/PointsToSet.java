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

package pascal.taie.analysis.pta.ci;

import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.util.collection.Sets;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Represents of points-to sets.
 */
class PointsToSet implements Iterable<Obj> {

    private final Set<Obj> set = Sets.newHybridSet();

    /**
     * Constructs an empty points-to set.
     */
    PointsToSet() {
    }

    /**
     * Constructs a points-to set containing one object.
     */
    PointsToSet(Obj obj) {
        addObject(obj);
    }

    /**
     * Adds an object to this set.
     *
     * @return true if this points-to set changed as a result of the call,
     * otherwise false.
     */
    boolean addObject(Obj obj) {
        return set.add(obj);
    }

    /**
     * @return true if this points-to set contains the given object, otherwise false.
     */
    boolean contains(Obj obj) {
        return set.contains(obj);
    }

    /**
     * @return whether this set if empty.
     */
    boolean isEmpty() {
        return set.isEmpty();
    }

    /**
     * @return the number of objects in this set.
     */
    int size() {
        return set.size();
    }

    /**
     * @return all objects in this set.
     */
    Stream<Obj> objects() {
        return set.stream();
    }

    /**
     * @return all objects in this set.
     */
    Set<Obj> getObjects() {
        return Collections.unmodifiableSet(set);
    }

    @Override
    public Iterator<Obj> iterator() {
        return set.iterator();
    }

    @Override
    public String toString() {
        return set.toString();
    }
}
