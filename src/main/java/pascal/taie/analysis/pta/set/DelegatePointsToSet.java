/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.analysis.pta.set;

import pascal.taie.analysis.pta.core.cs.CSObj;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Delegate points-to set to a concrete set implementation.
 */
abstract class DelegatePointsToSet implements PointsToSet {

    protected Set<CSObj> set;

    protected DelegatePointsToSet() {
        initializePointsToSet();
    }

    protected abstract void initializePointsToSet();

    @Override
    public boolean addObject(CSObj obj) {
        return set.add(obj);
    }

    @Override
    public boolean addAll(PointsToSet pts) {
        return set.addAll(pts.getObjects());
    }

    @Override
    public Collection<CSObj> getObjects() {
        return Collections.unmodifiableSet(set);
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public Stream<CSObj> stream() {
        return set.stream();
    }

    @Nonnull
    @Override
    public Iterator<CSObj> iterator() {
        return Collections.unmodifiableSet(set).iterator();
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
