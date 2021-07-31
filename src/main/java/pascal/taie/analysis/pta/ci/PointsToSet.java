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
import pascal.taie.util.collection.SetUtils;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

class PointsToSet {

    private final Set<Obj> set = SetUtils.newHybridSet();

    PointsToSet() {}

    PointsToSet(Obj obj) {
        addObject(obj);
    }

    boolean addObject(Obj obj) {
        return set.add(obj);
    }

    boolean isEmpty() {
        return set.isEmpty();
    }

    void forEach(Consumer<Obj> action) {
        set.forEach(action);
    }

    Stream<Obj> objects() {
        return set.stream();
    }

    @Override
    public String toString() {
        return set.toString();
    }
}
