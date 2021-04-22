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

public interface AnalysisResultHolder {

    /**
     * Store the analysis result with the analysis ID.
     */
    void storeResult(String id, Object result);

    /**
     * Given an analysis ID, return the corresponding results.
     */
    Object getResult(String id);

    /**
     * If this holder contains the result for given analysis ID,
     * then return the result, otherwise, return the given default result.
     */
    Object getResult(String id, Object defaultResult);

    /**
     * Clear all cached results.
     */
    void clearResult();
}
