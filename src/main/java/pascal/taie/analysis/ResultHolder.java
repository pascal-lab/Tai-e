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
 * The results of each analysis are associated with its id.
 * <p>
 * Currently, this interface is extends by {@link pascal.taie.ir.IR} and
 * held by {@link pascal.taie.World} for storing intra- and inter-procedural
 * analysis results, respectively.
 */
public interface ResultHolder {

    /**
     * Stores the analysis result with the analysis id.
     */
    <R> void storeResult(String id, R result);

    /**
     * Given an analysis id, returns the corresponding results.
     */
    <R> R getResult(String id);

    /**
     * If this holder contains the result for given analysis id,
     * then returns the result; otherwise, return the given default result.
     */
    <R> R getResult(String id, R defaultResult);

    /**
     * If this holder contains the result for given analysis id,
     * then returns the result; otherwise, supplier is used to create a result,
     * which is stored in the holder, and returned as the result of the call.
     */
    <R> R getResult(String id, Supplier<R> supplier);

    /**
     * Clears result of the analysis specified by given id.
     */
    void clearResult(String id);

    /**
     * Clears all cached results.
     */
    void clearAll();
}
