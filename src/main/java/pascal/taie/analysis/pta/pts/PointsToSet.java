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

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Representation of points-to sets that consist of {@link CSObj}.
 */
public interface PointsToSet extends Iterable<CSObj> {

    /**
     * Adds an object to this set.
     *
     * @return true if this points-to set changed as a result of the call,
     * otherwise false.
     */
    boolean addObject(CSObj obj);

    /**
     * Adds all objects in given pts to this set.
     *
     * @return true if this points-to set changed as a result of the call,
     * otherwise false.
     */
    boolean addAll(PointsToSet pts);

    /**
     * @return true if this set contains given object, otherwise false.
     */
    boolean contains(CSObj obj);

    /**
     * @return whether this set if empty.
     */
    boolean isEmpty();

    /**
     * @return the number of objects in this set.
     */
    int size();

    /**
     * @return all objects in this set.
     */
    Set<CSObj> getObjects();

    /**
     * @return all objects in this set.
     */
    Stream<CSObj> objects();

    @Override
    default Iterator<CSObj> iterator() {
        return objects().iterator();
    }
}
