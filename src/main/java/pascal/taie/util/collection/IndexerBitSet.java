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

package pascal.taie.util.collection;

import pascal.taie.util.Indexer;

import java.io.Serializable;

/**
 * This implementation leverages {@link Indexer} to take care of the mappings
 * between objects and indexes. The indexer itself acts as the context object.
 *
 * @param <E> type of elements
 * @see Indexer
 */
public class IndexerBitSet<E> extends GenericBitSet<E>
        implements Serializable {

    private final Indexer<E> indexer;

    public IndexerBitSet(Indexer<E> indexer, boolean isSparse) {
        super(isSparse);
        this.indexer = indexer;
    }

    @Override
    protected Object getContext() {
        return indexer;
    }

    @Override
    protected int getIndex(E o) {
        return indexer.getIndex(o);
    }

    @Override
    protected E getElement(int index) {
        return indexer.getObject(index);
    }

    @Override
    protected GenericBitSet<E> newSet() {
        return new IndexerBitSet<>(indexer, IBitSet.isSparse(bitSet));
    }
}
