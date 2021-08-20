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

package pascal.taie.util.collection;

/**
 * Represents a function that accepts one object and computes an index for it.
 * As a valid index, the returned numbers must be natural numbers.
 *
 * @see IndexMap
 */
@FunctionalInterface
public interface Indexer {

    int getIndex(Object o);
}
