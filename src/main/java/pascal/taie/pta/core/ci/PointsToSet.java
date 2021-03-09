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

package pascal.taie.pta.core.ci;

import pascal.taie.pta.ir.Obj;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

import static pascal.taie.util.CollectionUtils.newHybridSet;

class PointsToSet implements Iterable<Obj> {

    private final Set<Obj> set = newHybridSet();

    PointsToSet() {
    }

    PointsToSet(Obj obj) {
        addObject(obj);
    }

    public boolean addObject(Obj obj) {
        return set.add(obj);
    }

    public boolean isEmpty() {
        return set.isEmpty();
    }

    public Stream<Obj> stream() {
        return set.stream();
    }

    @Nonnull
    public Iterator<Obj> iterator() {
        return set.iterator();
    }

    public String toString() {
        return set.toString();
    }
}
