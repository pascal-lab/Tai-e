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


import pascal.taie.util.ObjectIdMapper;

/**
 * This implementation leverages {@link ObjectIdMapper} to take care of
 * the mappings between objects and indexes (i.e., the ids of the objects
 * maintained by the mapper). The mapper itself acts as the context object.
 *
 * @see ObjectIdMapper
 *
 * @param <E> type of elements
 */
public class MapperBitSet<E> extends GenericBitSet<E> {

    private final ObjectIdMapper<E> mapper;

    public MapperBitSet(ObjectIdMapper<E> mapper) {
        this.mapper = mapper;
    }

    public MapperBitSet(MapperBitSet<E> set) {
        super(set);
        this.mapper = set.mapper;
    }

    @Override
    public MapperBitSet<E> copy() {
        return new MapperBitSet<>(this);
    }

    @Override
    protected Object getContext() {
        return mapper;
    }

    @Override
    protected int getIndex(E o) {
        return mapper.getId(o);
    }

    @Override
    protected E getElement(int index) {
        return mapper.getObject(index);
    }
}
