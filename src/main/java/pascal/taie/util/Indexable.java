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

/**
 * The instances of the classes that implement this interface can provide
 * a unique <b>non-negative</b> index, so that they can be stored in efficient
 * data structures (e.g., bit set).
 * <p>
 * Note that the index of each object might NOT be globally unique,
 * when the indexes are unique within certain scope (e.g., the indexes
 * of local variables are unique only in the same method), and thus
 * the client code should use the indexes carefully.
 */
public interface Indexable {

    /**
     * @return index of this object. The index should be a non-negative integer.
     */
    int getIndex();
}
