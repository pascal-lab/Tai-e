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

package pascal.taie.analysis.dataflow.lattice;

import java.util.Set;

/**
 * Represents information for data-flow analysis.
 * A FlowSet is an element of a lattice.
 *
 * @param <E> Type for elements in this set.
 */
public interface FlowSet<E> extends Set<E> {

    /**
     * Unions other FlowSet into this FlowSet, returns the resulting FlowSet.
     */
    FlowSet<E> union(FlowSet<E> other);

    /**
     * Intersects other FlowSet and this FlowSet, returns the resulting FlowSet.
     */
    FlowSet<E> intersect(FlowSet<E> other);

    /**
     * Returns a duplication of this FlowSet.
     */
    FlowSet<E> duplicate();

    /**
     * Set this FlowSet to the same as the given one.
     */
    void setTo(FlowSet<E> other);

}
