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
 * The instances of the classes that implement this interface can provide
 * a unique <b>non-negative</b> index, so that they can be stored in efficient
 * data structures (e.g., bit set).
 *
 * Note that the index of each object might NOT be globally unique,
 * when the indexes are unique within certain scope (e.g., the indexes
 * of local variables are unique only in the same method), and thus
 * the client code should use the indexes carefully.
 */
public interface Indexable {

    /**
     * @return index of this object. The index should be a non-negative integer.
     */
    int getIndex();
}
