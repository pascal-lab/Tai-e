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
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Provides unmodifiable view collections.
 * TODO: doc complexity of specific operations.
 */
public final class Views {

    private Views() {
    }

    /**
     * Given a mapper function, creates an immutable view collection
     * for given collection. The elements of the resulting collection
     * are mapped from given collection.
     *
     * @param c        the backing collection
     * @param mapper   the function maps elements in backing collection to
     *                 the ones in view collection
     * @param contains the predicate function that check if view collection
     *                 contains given object.
     * @param <T>      type of elements in backing collection
     * @param <R>      type of elements in view collection
     * @return an immutable view collection.
     */
    public static <T, R> Collection<R> toMappedCollection(
            Collection<T> c, Function<T, R> mapper, Predicate<Object> contains) {
        return Collections.unmodifiableCollection(
                new MappedCollectionView<>(c, mapper, contains));
    }

    /**
     * Given a mapper function, creates an immutable view collection
     * for given collection. The elements of the resulting collection
     * are mapped from given collection.
     *
     * @param c        the backing collection
     * @param mapper   the function maps elements in backing collection to
     *                 the ones in view collection
     * @param <T>      type of elements in backing collection
     * @param <R>      type of elements in view collection
     * @return an immutable view collection.
     */
    public static <T, R> Collection<R> toMappedCollection(
            Collection<T> c, Function<T, R> mapper) {
        return Collections.unmodifiableCollection(
                new MappedCollectionView<>(c, mapper));
    }

    private static class MappedCollectionView<T, R> extends AbstractCollection<R> {

        private final Collection<T> backing;

        private final Function<T, R> mapper;

        private final Predicate<Object> contains;

        private MappedCollectionView(Collection<T> backing,
                                     Function<T, R> mapper, Predicate<Object> contains) {
            this.backing = backing;
            this.mapper = mapper;
            this.contains = contains;
        }

        private MappedCollectionView(Collection<T> backing, Function<T, R> mapper) {
            this.backing = backing;
            this.mapper = mapper;
            this.contains = super::contains;
        }

        @Override
        public boolean contains(Object o) {
            return contains.test(o);
        }

        @Nonnull
        @Override
        public Iterator<R> iterator() {
            return new Iterator<>() {

                private final Iterator<T> it = backing.iterator();

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public R next() {
                    return mapper.apply(it.next());
                }
            };
        }

        @Override
        public int size() {
            return backing.size();
        }
    }

    /**
     * Given a mapper function, creates an immutable view set for
     * given collection. Note that the uniqueness of elements in the
     * resulting set view is guaranteed by given collection {@code c}
     * and function {@code mapper}, not by the resulting set view itself.
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
    public static <T, R> Set<R> toMappedSet(
            Collection<T> c, Function<T, R> mapper, Predicate<Object> contains) {
        return Collections.unmodifiableSet(
                new MappedSetView<>(c, mapper, contains));
    }

    /**
     * Given a mapper function, creates an immutable view set for
     * given collection. Note that the uniqueness of elements in the
     * resulting set view is guaranteed by given collection {@code c}
     * and function {@code mapper}, not by the resulting set view itself.
     *
     * @param c        the backing collection
     * @param mapper   the function maps elements in backing collection to
     *                 the ones in view collection
     * @param <T>      type of elements in backing collection
     * @param <R>      type of elements in view collection
     * @return an immutable view set.
     */
    public static <T, R> Set<R> toMappedSet(Collection<T> c, Function<T, R> mapper) {
        return Collections.unmodifiableSet(new MappedSetView<>(c, mapper));
    }

    private static class MappedSetView<T, R> extends MappedCollectionView<T, R>
            implements Set<R> {

        private MappedSetView(Collection<T> backing,
                              Function<T, R> mapper, Predicate<Object> contains) {
            super(backing, mapper, contains);
        }

        private MappedSetView(Collection<T> backing, Function<T, R> mapper) {
            super(backing, mapper);
        }
    }

    /**
     * Given a filter function, creates an immutable view collection for
     * given collection. The elements in the original collection that do not
     * satisfy the filter function will be absent in the resulting collection.
     *
     * @param backing the backing collection
     * @param filter  the function that decides which elements stay in the
     *                resulting collection
     * @param <T>     type of elements in the resulting collection
     * @return an immutable view collection.
     */
    public static <T> Collection<T> toFilteredCollection(
            Collection<? extends T> backing, Predicate<T> filter) {
        return Collections.unmodifiableCollection(
                new FilteredCollectionView<>(backing, filter));
    }

    private static class FilteredCollectionView<T> extends AbstractCollection<T> {

        private final Collection<? extends T> backing;

        private final Predicate<T> filter;

        private FilteredCollectionView(Collection<? extends T> backing, Predicate<T> filter) {
            this.backing = backing;
            this.filter = filter;
        }

        @Override
        public boolean contains(Object o) {
            return backing.contains(o) && filter.test((T) o);
        }

        @Nonnull
        @Override
        public Iterator<T> iterator() {
            return new FilteredIterator();
        }

        private class FilteredIterator implements Iterator<T> {

            private final Iterator<? extends T> it = backing.iterator();

            private T next;

            private FilteredIterator() {
                advance();
            }

            private void advance() {
                next = null;
                T e;
                while (it.hasNext()) {
                    e = it.next();
                    if (filter.test(e)) {
                        next = e;
                        break;
                    }
                }
            }

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public T next() {
                T e = next;
                if (e == null) {
                    throw new NoSuchElementException();
                }
                advance();
                return e;
            }
        }

        @Override
        public int size() {
            int size = 0;
            for (T e : backing) {
                if (filter.test(e)) {
                    ++size;
                }
            }
            return size;
        }
    }
}
