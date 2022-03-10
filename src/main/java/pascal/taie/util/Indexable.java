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

package pascal.taie.util;

/**
 * Implementing this interface allows an object to return a unique index,
 * so that it could be stored in efficient data structures (e.g., bit set).
 * <p>
 * Note that the index may not be globally unique (this depends on the
 * implementations of this class), so you need to use {@link #getIndex()}
 * with care.
 */
public interface Indexable {

    /**
     * @return the index of this object.
     */
    int getIndex();
}
