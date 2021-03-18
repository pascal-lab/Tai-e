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

package pascal.taie.analysis.oldpta.set;

import pascal.taie.analysis.oldpta.core.cs.CSObj;

import java.util.Collection;
import java.util.stream.Stream;

public interface PointsToSet extends Iterable<CSObj> {

    boolean addObject(CSObj obj);

    boolean addAll(PointsToSet pts);

    Collection<CSObj> getObjects();

    boolean isEmpty();

    int size();

    Stream<CSObj> stream();
}
