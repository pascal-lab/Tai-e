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

package pascal.taie.analysis.pta.pts;

import pascal.taie.analysis.pta.core.cs.element.CSObj;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Delegates points-to set to a concrete set implementation.
 */
abstract class DelegatePointsToSet implements PointsToSet {

    protected final Set<CSObj> set;

    DelegatePointsToSet(Set<CSObj> set) {
        this.set = set;
    }

    @Override
    public boolean addObject(CSObj obj) {
        return set.add(obj);
    }

    @Override
    public boolean addAll(PointsToSet pts) {
        boolean changed = false;
        for (CSObj o : pts) {
            changed |= addObject(o);
        }
        return changed;
    }

    @Override
    public boolean contains(CSObj obj) {
        return set.contains(obj);
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public Set<CSObj> getObjects() {
        return Collections.unmodifiableSet(set);
    }

    @Override
    public Stream<CSObj> objects() {
        return set.stream();
    }

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public String toString() {
        return set.toString();
    }
}
