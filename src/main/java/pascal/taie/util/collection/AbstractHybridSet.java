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
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * This set supports hybrid of two set implementations, where one is
 * efficient for small set and another one is efficient for large set.
 * <p>
 * When the number of elements contained in this set succeeds a threshold,
 * it will automatically upgrade the set implementation to the one that is
 * efficient for large set. Moreover, empty sets and singleton sets are
 * represented with just a reference.
 * <p>
 * Elements added to this set cannot be null.
 * <p>
 * By default, this set uses {@link ArraySet} for small set.
 *
 * @param <E> type of elements
 */
public abstract class AbstractHybridSet<E> extends AbstractSetEx<E>
        implements Serializable {

    // invariant: at most one of singleton and set is non-null

    private static final String NULL_MESSAGE = "HybridSet does not permit null values";

    /**
     * Default size of small set, which acts as the threshold for
     * the number of items necessary for the small set to become a large set.
     */
    private static final int SMALL_SIZE = 8;

    /**
     * The singleton value. Null if not a singleton.
     */
    protected E singleton;

    /**
     * The set containing the elements. Null if the set is not used.
     */
    protected Set<E> set;

    /**
     * Whether the set implementation is the one for large set.
     */
    protected boolean isLargeSet = false;

    /**
     * Constructs a new hybrid set.
     */
    protected AbstractHybridSet() {
    }

    /**
     * Constructs a new hybrid set from the given collection.
     */
    protected AbstractHybridSet(Collection<E> c) {
        addAll(c);
    }

    /**
     * @return the threshold between sizes of small set and large set.
     * When number of elements exceeds the threshold, set should be upgraded
     * to large set.
     */
    protected int getThreshold() {
        return SMALL_SIZE;
    }

    /**
     * Creates a small set.
     */
    protected Set<E> newSmallSet() {
        return new ArraySet<>(getThreshold());
    }

    /**
     * Creates a large set.
     *
     * @param initialCapacity initial capacity of the resulting set.
     */
    protected abstract Set<E> newLargeSet(int initialCapacity);

    @Override
    public boolean add(E e) {
        Objects.requireNonNull(e, NULL_MESSAGE);
        if (singleton != null) {
            if (singleton.equals(e)) {
                return false;
            }
            upgradeToSmallSet();
        }
        if (set != null) {
            if (!isLargeSet && set.size() + 1 > getThreshold()) {
                upgradeToLargeSet(getThreshold() * 2);
            }
            return set.add(e);
        }
        singleton = e;
        return true;
    }

    private void upgradeToSmallSet() {
        set = newSmallSet();
        if (singleton != null) {
            set.add(singleton);
            singleton = null;
        }
    }

    private void upgradeToLargeSet(int initialCapacity) {
        assert !isLargeSet;
        Set<E> origin = set;
        set = newLargeSet(initialCapacity);
        if (singleton != null) {
            set.add(singleton);
            singleton = null;
        }
        if (origin != null) {
            set.addAll(origin);
        }
        isLargeSet = true;
    }

    @Override
    public boolean addAll(@Nonnull Collection<? extends E> c) {
        if (!isLargeSet) {
            int cSize = c.size();
            if (cSize == 0) {
                return false;
            }
            int maxNewSize = cSize + size();
            int threshold = getThreshold();
            if (set == null) {
                if (maxNewSize == 1) {
                    assert singleton == null;
                    E e = CollectionUtils.getOne(c);
                    singleton = Objects.requireNonNull(e, NULL_MESSAGE);
                    return true;
                } else if (maxNewSize <= threshold) {
                    upgradeToSmallSet();
                } else {
                    upgradeToLargeSet(maxNewSize + threshold);
                }
            } else if (maxNewSize > threshold) {
                upgradeToLargeSet(maxNewSize + threshold);
            }
        }
        if (!(c instanceof AbstractHybridSet)) {
            c.forEach(e -> Objects.requireNonNull(e, NULL_MESSAGE));
        }
        return set.addAll(c);
    }

    @Override
    public void clear() {
        if (singleton != null) {
            singleton = null;
        }
        if (set != null) {
            set.clear();
        }
    }

    @Override
    public boolean contains(Object o) {
        if (singleton != null) {
            return singleton.equals(o);
        }
        if (set != null) {
            return set.contains(o);
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isEmpty() {
        if (singleton != null) {
            return false;
        }
        if (set != null) {
            return set.isEmpty();
        }
        return true;
    }

    @Override
    public int size() {
        if (singleton != null) {
            return 1;
        }
        if (set != null) {
            return set.size();
        }
        return 0;
    }

    @Override
    @Nonnull
    public Iterator<E> iterator() {
        if (singleton != null) {
            return new Iterator<>() {

                boolean done;

                @Override
                public boolean hasNext() {
                    return !done;
                }

                @Override
                public E next() {
                    if (done) {
                        throw new NoSuchElementException();
                    }
                    done = true;
                    return singleton;
                }

                @Override
                public void remove() {
                    if (done && singleton != null) {
                        singleton = null;
                    } else {
                        throw new IllegalStateException();
                    }
                }
            };
        }
        if (set != null) {
            return set.iterator();
        }
        return Collections.emptyIterator();
    }

    @Override
    public boolean remove(Object o) {
        if (singleton != null) {
            if (singleton.equals(o)) {
                singleton = null;
                return true;
            }
            return false;
        }
        if (set != null) {
            return set.remove(o);
        }
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean changed = false;
        for (Object o : c) {
            changed |= remove(o);
        }
        return changed;
    }

    @Override
    public boolean retainAll(@Nonnull Collection<?> c) {
        boolean changed = false;
        for (Iterator<E> it = iterator(); it.hasNext(); ) {
            if (!c.contains(it.next())) {
                it.remove();
                changed = true;
            }
        }
        return changed;
    }

    @Override
    @Nonnull
    public Object[] toArray() {
        if (singleton != null) {
            Object[] a = new Object[1];
            a[0] = singleton;
            return a;
        }
        if (set != null) {
            return set.toArray();
        }
        return new Object[0];
    }

    @Override
    @Nonnull
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(@Nonnull T[] a) {
        if (singleton != null) {
            if (a.length < 1) {
                a = (T[]) Array.newInstance(a.getClass().getComponentType(), 1);
            }
            a[0] = (T) singleton; // TODO: throw ArrayStoreException if not T :> V
            return a;
        }
        if (set != null) {
            return set.toArray(a);
        }
        Arrays.fill(a, null);
        return a;
    }

    @Override
    public int hashCode() { // see contract for Set.hashCode
        if (singleton != null) {
            return singleton.hashCode();
        }
        if (set != null) {
            return set.hashCode();
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Set<?> s)) {
            return false;
        }
        if (size() != s.size()) {
            return false;
        }
        // TODO: special support for singletons...
        if (hashCode() != s.hashCode()) {
            return false;
        }
        return containsAll(s);
    }
}
