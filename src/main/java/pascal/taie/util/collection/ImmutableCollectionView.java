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

import pascal.taie.util.collection.CollectionView;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;

public class ImmutableCollectionView<From, To> implements CollectionView<From, To> {

    private final Collection<From> collection;

    private final Function<From, To> mapper;

    ImmutableCollectionView(Collection<From> collection, Function<From, To> mapper) {
        this.collection = collection;
        this.mapper = mapper;
    }

    @Override
    public int size() {
        return collection.size();
    }

    @Override
    public boolean isEmpty() {
        return collection.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException(
                "contains() currently is not supported");
    }

    @Nonnull
    @Override
    public Iterator<To> iterator() {
        return new Iterator<To>() {

            private final Iterator<From> iter = collection.iterator();

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public To next() {
                return mapper.apply(iter.next());
            }
        };
    }

    @Nonnull
    @Override
    public Object[] toArray() {
        return collection.stream()
                .map(mapper)
                .toArray();
    }

    @Nonnull
    @Override
    public <T> T[] toArray(@Nonnull T[] a) {
        throw new UnsupportedOperationException("ImmutableCollectionView does not support toArray(T[]) yet.");
    }

    @Override
    public boolean add(To to) {
        throw new UnsupportedOperationException("ImmutableCollectionView cannot be modified.");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("ImmutableCollectionView cannot be modified.");
    }

    @Override
    public boolean containsAll(@Nonnull Collection<?> c) {
        return collection.containsAll(c);
    }

    @Override
    public boolean addAll(@Nonnull Collection<? extends To> c) {
        throw new UnsupportedOperationException("ImmutableCollectionView cannot be modified.");
    }

    @Override
    public boolean removeAll(@Nonnull Collection<?> c) {
        throw new UnsupportedOperationException("ImmutableCollectionView cannot be modified.");
    }

    @Override
    public boolean retainAll(@Nonnull Collection<?> c) {
        throw new UnsupportedOperationException("ImmutableCollectionView cannot be modified.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("ImmutableCollectionView cannot be modified.");
    }
}
