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

package pascal.taie.analysis.pta.core.cs.context;

/**
 * Factory of contexts, which provides convenient APIs to create contexts.
 *
 * @param <T> type of elements of created contexts.
 */
public interface ContextFactory<T> {

    /**
     * @return the empty context.
     */
    Context getEmptyContext();

    /**
     * @return the context with one element.
     */
    Context make(T elem);

    /**
     * @return the context that consists of given elements.
     */
    Context make(T... elems);

    /**
     * @return a context with last k elements of given context.
     */
    Context makeLastK(Context context, int k);

    /**
     * Constructs a context by appending a context element to a parent context.
     * The length of the resulting context will be restricted by given limit.
     *
     * @return the resulting context.
     */
    Context append(Context parent, T elem, int limit);
}
