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

import java.util.Set;
import java.util.stream.Stream;

/**
 * Immutable empty points-to set.
 */
enum EmptyPointsToSet implements PointsToSet {

    INSTANCE;

    @Override
    public boolean addObject(CSObj obj) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(PointsToSet pts) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PointsToSet addAllDiff(PointsToSet pts) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(CSObj obj) {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public Set<CSObj> getObjects() {
        return Set.of();
    }

    @Override
    public Stream<CSObj> objects() {
        return Stream.empty();
    }

    @Override
    public PointsToSet copy() {
        return this;
    }
}
