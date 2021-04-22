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

import java.util.Map;

import static pascal.taie.util.collection.CollectionUtils.newHybridMap;

/**
 * Abstract implementation for {@link AnalysisResultHolder}.
 */
public abstract class AbstractHolder implements AnalysisResultHolder {

    /**
     * Map from analysis ID to the corresponding analysis result.
     */
    private final Map<String, Object> results = newHybridMap();

    public void storeResult(String id, Object result) {
        results.put(id, result);
    }

    public Object getResult(String id) {
        return results.get(id);
    }

    public Object getResult(String id, Object defaultResult) {
        return results.getOrDefault(id, defaultResult);
    }

    public void clearResult() {
        results.clear();
    }
}
