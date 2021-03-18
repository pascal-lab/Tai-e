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

package pascal.taie.analysis.oldpta.core.ci;

import pascal.taie.analysis.oldpta.ir.Obj;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

import static pascal.taie.util.collection.CollectionUtils.newHybridSet;

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
