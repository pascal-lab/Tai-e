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

package panda.dataflow.lattice;

import java.util.Collection;
import java.util.HashSet;

/**
 * A HashSet-based implementation for FlowSet.
 * This implementation is simple and fast for various set operations.
 *
 * @param <E> Type for elements in this set.
 */
public class HashFlowSet<E> extends HashSet<E> implements FlowSet<E> {

    public HashFlowSet() {
        super();
    }

    public HashFlowSet(Collection<E> elements) {
        super(elements);
    }

    @Override
    public FlowSet<E> union(FlowSet<E> other) {
        addAll(other);
        return this;
    }

    @Override
    public FlowSet<E> intersect(FlowSet<E> other) {
        retainAll(other);
        return this;
    }

    @Override
    public FlowSet<E> duplicate() {
        return new HashFlowSet<>(this);
    }

    @Override
    public void setTo(FlowSet<E> other) {
        clear();
        addAll(other);
    }

}
