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

import java.util.stream.Stream;

/**
 * Representation of points-to sets that consist of {@link CSObj}.
 */
public interface PointsToSet extends Iterable<CSObj> {

    /**
     * Adds an object to this set.
     * @return if the add operation changes this set.
     */
    boolean addObject(CSObj obj);

    /**
     * Adds all objects in given pts to this set.
     * @return if the add operation changes this set.
     */
    boolean addAll(PointsToSet pts);

    /**
     * @return if this set contains given object.
     */
    boolean contains(CSObj obj);

    /**
     * @return is this set if empty.
     */
    boolean isEmpty();

    /**
     * @return the number of objects in this set.
     */
    int size();

    /**
     * @return a stream of all objects in this set.
     */
    Stream<CSObj> objects();
}
