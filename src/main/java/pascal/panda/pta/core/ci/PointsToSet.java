/*
 * Panda - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Panda is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.panda.pta.core.ci;

import pascal.panda.pta.element.Obj;
import pascal.panda.util.HybridArrayHashSet;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

class PointsToSet implements Iterable<Obj> {

    private final Set<Obj> set = new HybridArrayHashSet<>();

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
