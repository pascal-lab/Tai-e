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

package pascal.taie.util.collection;

import pascal.taie.util.Indexer;

/**
 * This implementation leverages {@link Indexer} to take care of the mappings
 * between objects and indexes. The indexer itself acts as the context object.
 *
 * @see Indexer
 *
 * @param <E> type of elements
 */
public class IndexerBitSet<E> extends GenericBitSet<E> {

    private final Indexer<E> indexer;

    public IndexerBitSet(Indexer<E> indexer) {
        this(indexer, false);
    }

    public IndexerBitSet(Indexer<E> indexer, boolean sparse) {
        super(sparse);
        this.indexer = indexer;
    }

    private IndexerBitSet(IndexerBitSet<E> set) {
        super(set);
        this.indexer = set.indexer;
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
    public IndexerBitSet<E> copy() {
        return new IndexerBitSet<>(this);
    }
}
