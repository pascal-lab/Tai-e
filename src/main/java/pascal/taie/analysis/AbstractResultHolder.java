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
    public <R> void storeResult(String id, R result) {
        results.put(id, result);
    }

    @Override
    public <R> R getResult(String id) {
        return (R) results.get(id);
    }

    @Override
    public <R> R getResult(String id, R defaultResult) {
        return (R) results.getOrDefault(id, defaultResult);
    }

    @Override
    public <R> R getResult(String id, Supplier<R> supplier) {
        return (R) results.computeIfAbsent(id, unused -> supplier.get());
    }

    @Override
    public void clearResult(String id) {
        results.remove(id);
    }

    @Override
    public void clearAll() {
        results.clear();
    }
}
