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

import javax.annotation.Nonnull;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Provides unmodifiable view collections.
 */
public class Views {

    private Views() {
    }

    /**
     * Creates an immutable view collection for given collection.
     *
     * @param c        the backing collection
     * @param mapper   the function maps elements in backing collection to
     *                 the ones in view collection
     * @param contains the predicate function that check if view collection
     *                 contains given object
     * @param <T>      type of elements in backing collection
     * @param <R>      type of elements in view collection
     * @return an immutable view collection.
     */
    public static <T, R> Collection<R> toCollection(
            Collection<T> c, Function<T, R> mapper, Predicate<Object> contains) {
        return Collections.unmodifiableCollection(
                new CollectionView<>(c, mapper, contains));
    }

    private static class CollectionView<T, R> extends AbstractCollection<R> {

        private final Collection<T> backing;

        private final Function<T, R> mapper;

        private final Predicate<Object> contains;

        private CollectionView(Collection<T> backing,
                               Function<T, R> mapper, Predicate<Object> contains) {
            this.backing = backing;
            this.mapper = mapper;
            this.contains = contains;
        }

        @Override
        public boolean contains(Object o) {
            return contains.test(o);
        }

        @Nonnull
        @Override
        public Iterator<R> iterator() {
            return new Iterator<>() {

                private final Iterator<T> i = backing.iterator();

                @Override
                public boolean hasNext() {
                    return i.hasNext();
                }

                @Override
                public R next() {
                    return mapper.apply(i.next());
                }
            };
        }

        @Override
        public int size() {
            return backing.size();
        }
    }

    /**
     * Creates an immutable view set for given collection.
     * Note that the uniqueness of elements in the resulting set view
     * is guaranteed by given collection {@code c} and function {@code mapper},
     * not by the resulting set view itself.
     *
     * @param c        the backing collection
     * @param mapper   the function maps elements in backing collection to
     *                 the ones in view collection
     * @param contains the predicate function that check if view collection
     *                 contains given object
     * @param <T>      type of elements in backing collection
     * @param <R>      type of elements in view collection
     * @return an immutable view set.
     */
    public static <T, R> Set<R> toSet(
            Collection<T> c, Function<T, R> mapper, Predicate<Object> contains) {
        return Collections.unmodifiableSet(
                new SetView<>(c, mapper, contains));
    }

    private static class SetView<T, R> extends CollectionView<T, R>
            implements Set<R> {

        private SetView(Collection<T> backing,
                        Function<T, R> mapper, Predicate<Object> contains) {
            super(backing, mapper, contains);
        }
    }
}
