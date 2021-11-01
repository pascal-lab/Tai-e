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
import java.util.function.Supplier;

import static pascal.taie.util.collection.Maps.newHybridMap;

/**
 * Implementation for {@link ResultHolder}.
 */
public abstract class AbstractResultHolder implements ResultHolder {

    /**
     * Map from analysis ID to the corresponding analysis result.
     */
    private final Map<String, Object> results = newHybridMap();

    @Override
    public <R> void storeResult(String key, R result) {
        results.put(key, result);
    }

    @Override
    public <R> R getResult(String key) {
        return (R) results.get(key);
    }

    @Override
    public <R> R getResult(String key, R defaultResult) {
        return (R) results.getOrDefault(key, defaultResult);
    }

    @Override
    public <R> R getResult(String key, Supplier<R> supplier) {
        return (R) results.computeIfAbsent(key, unused -> supplier.get());
    }

    @Override
    public void clearResult(String key) {
        results.remove(key);
    }

    @Override
    public void clearAll() {
        results.clear();
    }
}
