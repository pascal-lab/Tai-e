/*
 * Panda - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Panda is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.panda.util;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * Hybrid of array and hash set.
 * Small maps are represented as array set; above a certain threshold a hash set is used instead.
 * Moreover, empty sets and singleton sets are represented with just a reference.
 * Elements cannot be null.
 */
public final class HybridArrayHashSet<E> implements Set<E>, Serializable {

    // invariant: at most one of singleton, array and hashset is non-null

    private static final String NULL_MESSAGE = "HybridArrayHashSet does not permit null keys";
    /**
     * Default threshold for the number of items necessary for the array set
     * to become a hash set.
     */
    private static final int ARRAY_SET_SIZE = 8;

    /**
     * The singleton value. Null if not a singleton.
     */
    private E singleton;

    /**
     * The array set set with the items. Null if the array set is not used.
     */
    private ArraySet<E> arraySet;

    /**
     * The hash set with the items. Null if the hash set is not used.
     */
    private HashSet<E> hashSet;

    /**
     * Constructs a new hybrid set.
     */
    public HybridArrayHashSet() {
        // do nothing
    }

    /**
     * Constructs a new hybrid set from the given collection.
     */
    public HybridArrayHashSet(Collection<E> c) {
        addAll(c);
    }

    @Override
    public boolean add(E e) {
        Objects.requireNonNull(e, NULL_MESSAGE);
        if (singleton != null) {
            if (singleton.equals(e)) {
                return false;
            }
            convertSingletonToArraySet();
        }
        if (arraySet != null) {
            if (arraySet.size() + 1 <= ARRAY_SET_SIZE) {
                return arraySet.add(e);
            }
            convertArraySetToHashSet();
        }
        if (hashSet != null) {
            return hashSet.add(e);
        }
        singleton = e;
        return true;
    }

    private void convertSingletonToArraySet() {
        arraySet = new ArraySet<>(ARRAY_SET_SIZE);
        arraySet.add(singleton);
        singleton = null;
    }

    private void convertArraySetToHashSet() {
        hashSet = new HashSet<>(ARRAY_SET_SIZE * 2);
        hashSet.addAll(arraySet);
        arraySet = null;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        int c_size = c.size();
        if (c_size == 0) {
            return false;
        }
        int max_new_size = c_size + size();
        if (arraySet == null && hashSet == null && max_new_size == 1) {
            E e = c.iterator().next();
            singleton = Objects.requireNonNull(e, NULL_MESSAGE);
            return true;
        }
        if (!(c instanceof HybridArrayHashSet)) {
            c.forEach(e -> Objects.requireNonNull(e, NULL_MESSAGE));
        }
        if (arraySet == null && hashSet == null
                && max_new_size <= ARRAY_SET_SIZE) {
            if (singleton != null)
                convertSingletonToArraySet();
            else {
                arraySet = new ArraySet<>(ARRAY_SET_SIZE);
            }
        }
        if (arraySet != null) {
            if (max_new_size <= ARRAY_SET_SIZE) {
                return arraySet.addAll(c);
            } else {
                convertArraySetToHashSet();
            }
        }
        if (hashSet == null) {
            hashSet = new HashSet<>(ARRAY_SET_SIZE + max_new_size);
        }
        if (singleton != null) {
            hashSet.add(singleton);
            singleton = null;
        }
        return hashSet.addAll(c);
    }

    @Override
    public void clear() {
        if (singleton != null) {
            singleton = null;
        }
        if (arraySet != null) {
            arraySet.clear();
        }
        if (hashSet != null) {
            hashSet.clear();
        }
    }

    @Override
    public boolean contains(Object o) {
        if (singleton != null) {
            return singleton.equals(o);
        }
        if (arraySet != null) {
            return arraySet.contains(o);
        }
        if (hashSet != null) {
            return hashSet.contains(o);
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
        if (arraySet != null) {
            return arraySet.isEmpty();
        }
        if (hashSet != null) {
            return hashSet.isEmpty();
        }
        return true;
    }

    @Override
    public int size() {
        if (singleton != null) {
            return 1;
        }
        if (arraySet != null) {
            return arraySet.size();
        }
        if (hashSet != null) {
            return hashSet.size();
        }
        return 0;
    }

    @Nonnull
    @Override
    public Iterator<E> iterator() {
        if (singleton != null) {
            return new Iterator<E>() {

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
        if (arraySet != null) {
            return arraySet.iterator();
        }
        if (hashSet != null) {
            return hashSet.iterator();
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
        if (arraySet != null) {
            return arraySet.remove(o);
        }
        if (hashSet != null) {
            return hashSet.remove(o);
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
    public Object[] toArray() {
        if (singleton != null) {
            Object[] a = new Object[1];
            a[0] = singleton;
            return a;
        }
        if (arraySet != null) {
            return arraySet.toArray();
        }
        if (hashSet != null) {
            return hashSet.toArray();
        }
        return new Object[0];
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(@Nonnull T[] a) {
        if (singleton != null) {
            if (a.length < 1)
                a = (T[]) Array.newInstance(a.getClass().getComponentType(), 1);
            a[0] = (T) singleton; // TODO: throw ArrayStoreException if not T :> V
            return a;
        }
        if (arraySet != null) {
            //noinspection SuspiciousToArrayCall
            return arraySet.toArray(a);
        }
        if (hashSet != null) {
            //noinspection SuspiciousToArrayCall
            return hashSet.toArray(a);
        }
        Arrays.fill(a, null);
        return a;
    }

    @Override
    public int hashCode() { // see contract for Set.hashCode
        if (singleton != null) {
            return singleton.hashCode();
        }
        if (arraySet != null) {
            return arraySet.hashCode();
        }
        if (hashSet != null) {
            return hashSet.hashCode();
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Set<?>)) {
            return false;
        }
        Set<?> s = (Set<?>) obj;
        if (size() != s.size()) {
            return false;
        }
        // TODO: special support for singletons...
        if (hashCode() != s.hashCode()) {
            return false;
        }
        return containsAll(s);
    }

    @Override
    public String toString() {
        if (singleton != null) {
            return "[" + singleton + ']';
        }
        if (arraySet != null) {
            return arraySet.toString();
        }
        if (hashSet != null) {
            return hashSet.toString();
        }
        return "[]";
    }
}
