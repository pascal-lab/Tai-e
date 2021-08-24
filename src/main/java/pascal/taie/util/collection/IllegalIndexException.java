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
 * Thrown when the index returned by an indexer exceeds the bound of
 * the related index map.
 *
 * @see IndexMap
 * @see Indexer
 */
public class IllegalIndexException extends RuntimeException {

    public IllegalIndexException(String message) {
        super(message);
    }
}
