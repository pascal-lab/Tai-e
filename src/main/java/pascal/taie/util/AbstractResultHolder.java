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

import pascal.taie.util.collection.Maps;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Map-based implementation for {@link ResultHolder}.
 */
public abstract class AbstractResultHolder implements ResultHolder {

    /**
     * Map from analysis ID to the corresponding analysis result.
     */
    private final Map<String, Object> results = Maps.newHybridMap();

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
    public Collection<String> getKeys() {
        return results.keySet();
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
