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

import javax.annotation.Nonnull;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
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
     * @param c      the backing collection
     * @param mapper the function maps elements in backing collection to
     *               the ones in view collection
     * @param <T>    type of elements in backing collection
     * @param <R>    type of elements in view collection
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

        @Override
        @Nonnull
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
     * given collection.
     * <p>
     * WARNING: the uniqueness of elements in the resulting set view is
     * guaranteed by given collection {@code c} and function {@code mapper},
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
    public static <T, R> Set<R> toMappedSet(
            Collection<T> c, Function<T, R> mapper, Predicate<Object> contains) {
        return Collections.unmodifiableSet(
                new MappedSetView<>(c, mapper, contains));
    }

    /**
     * Given a mapper function, creates an immutable view set for
     * given collection.
     * <p>
     * WARNING: the uniqueness of elements in the resulting set view is
     * guaranteed by given collection {@code c} and function {@code mapper},
     * not by the resulting set view itself.
     *
     * @param c      the backing collection
     * @param mapper the function maps elements in backing collection to
     *               the ones in view collection
     * @param <T>    type of elements in backing collection
     * @param <R>    type of elements in view collection
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

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof Set)) {
                return false;
            }
            Collection<?> c = (Collection<?>) o;
            if (c.size() != size()) {
                return false;
            }
            try {
                return containsAll(c);
            } catch (ClassCastException | NullPointerException unused) {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int h = 0;
            for (R obj : this) {
                h += Objects.hashCode(obj);
            }
            return h;
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

        @Override
        @Nonnull
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

    /**
     * Given two sets, creates an immutable view set consisting of elements
     * of the two sets.
     * <p>
     * WARNING: this implementation simply treats two sets as one set and
     * does not guarantee the uniqueness of elements. It is responsibility of
     * the client code to guarantee that the elements in {@code set1} and
     * {@code set2} are disjoint.
     *
     * @param set1 the first set to combine
     * @param set2 the second set to combine
     * @param <T>  type of elements
     * @return an immutable view set containing elements of {@code set1}
     * and {@code set2}
     */
    public static <T> Set<T> toCombinedSet(
            Set<? extends T> set1, Set<? extends T> set2) {
        return Collections.unmodifiableSet(new CombinedSetView<>(set1, set2));
    }

    private static class CombinedSetView<T> extends AbstractSet<T> {

        private final Set<? extends T> set1;

        private final Set<? extends T> set2;

        private CombinedSetView(Set<? extends T> set1, Set<? extends T> set2) {
            this.set1 = set1;
            this.set2 = set2;
        }

        @Override
        public boolean contains(Object o) {
            return set1.contains(o) || set2.contains(o);
        }

        @Override
        @Nonnull
        public Iterator<T> iterator() {
            return new Iterator<>() {

                private final Iterator<? extends T> it1 = set1.iterator();

                private final Iterator<? extends T> it2 = set2.iterator();

                @Override
                public boolean hasNext() {
                    return it1.hasNext() || it2.hasNext();
                }

                @Override
                public T next() {
                    if (it1.hasNext()) {
                        return it1.next();
                    } else if (it2.hasNext()) {
                        return it2.next();
                    } else {
                        throw new NoSuchElementException();
                    }
                }
            };
        }

        @Override
        public boolean isEmpty() {
            return set1.isEmpty() && set2.isEmpty();
        }

        @Override
        public int size() {
            return set1.size() + set2.size();
        }
    }
}
