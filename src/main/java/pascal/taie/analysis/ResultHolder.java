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

package pascal.taie.analysis;

import java.util.function.Supplier;

/**
 * The holder object of analysis results.
 * Each result is associated with a key (of String).
 */
public interface ResultHolder {

    /**
     * Stores the analysis result with the key.
     */
    <R> void storeResult(String key, R result);

    /**
     * Given a key, returns the corresponding results.
     */
    <R> R getResult(String key);

    /**
     * If this holder contains the result for given key,
     * then returns the result; otherwise, return the given default result.
     */
    <R> R getResult(String key, R defaultResult);

    /**
     * If this holder contains the result for given key,
     * then returns the result; otherwise, supplier is used to create a result,
     * which is stored in the holder, and returned as the result of the call.
     */
    <R> R getResult(String key, Supplier<R> supplier);

    /**
     * Clears result of the analysis specified by given key.
     */
    void clearResult(String key);

    /**
     * Clears all cached results.
     */
    void clearAll();
}
