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
 * NOTE: abstract class should NOT implement this interface.
 *
 * @param <T> type of copy object
 */
public interface Copyable<T> {

    /**
     * Creates and returns a copy of this object.
     *
     * @return a copy of this object.
     */
    T copy();
}
