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

import java.io.Serializable;

/**
 * An indexer assigns each object a unique index, so that the objects
 * can be stored in efficient data structures. Symmetrically, an indexer
 * can map an index to the corresponding object.
 * <p>
 * Note that each object in the same indexer has a unique index,
 * but different indexers may map different objects (indexes)
 * to the same index (object), so it should be used with care.
 * <p>
 * The objects in an indexer {@code i} should preserve the invariant:
 * <code>e.equals(i.getObject(i.getIndex(e)))</code>.
 *
 * @param <E> type of objects to be indexed
 */
public interface Indexer<E> extends Serializable {

    /**
     * @return the index of the given object.
     */
    int getIndex(E o);

    /**
     * @return the corresponding object of the given index.
     */
    E getObject(int index);
}
