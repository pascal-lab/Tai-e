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

import java.util.Collection;
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
     * @return {@code true} if the holder contains the result for given id.
     */
    boolean hasResult(String id);

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
     * @return all keys in the holder.
     */
    Collection<String> getKeys();

    /**
     * Clears result of the analysis specified by given key.
     */
    void clearResult(String key);

    /**
     * Clears all cached results.
     */
    void clearAll();
}
