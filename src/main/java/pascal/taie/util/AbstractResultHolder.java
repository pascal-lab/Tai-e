/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
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
    private final transient Map<String, Object> results = Maps.newHybridMap();

    @Override
    public <R> void storeResult(String key, R result) {
        results.put(key, result);
    }

    @Override
    public boolean hasResult(String key) {
        return results.containsKey(key);
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
        return (R) results.computeIfAbsent(key, __ -> supplier.get());
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
