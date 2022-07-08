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

package pascal.taie.analysis.pta.pts;

import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.util.collection.SetEx;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Delegates points-to set to a concrete set implementation.
 */
abstract class DelegatePointsToSet implements PointsToSet {

    protected final SetEx<CSObj> set;

    DelegatePointsToSet(SetEx<CSObj> set) {
        this.set = set;
    }

    @Override
    public boolean addObject(CSObj obj) {
        return set.add(obj);
    }

    @Override
    public boolean addAll(PointsToSet pts) {
        if (pts instanceof DelegatePointsToSet other) {
            return set.addAll(other.set);
        } else {
            boolean changed = false;
            for (CSObj o : pts) {
                changed |= addObject(o);
            }
            return changed;
        }
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

    @Override
    public PointsToSet addAllDiff(PointsToSet pts) {
        Set<CSObj> otherSet = pts instanceof DelegatePointsToSet other ?
                other.set : pts.getObjects();
        return newSet(set.addAllDiff(otherSet));
    }

    @Override
    public PointsToSet copy() {
        return newSet(set.copy());
    }

    protected abstract PointsToSet newSet(SetEx<CSObj> set);
}
