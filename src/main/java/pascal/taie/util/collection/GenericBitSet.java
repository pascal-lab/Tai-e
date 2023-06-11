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

import pascal.taie.util.Hashes;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Bit set based implementation of {@link java.util.Set}.
 * <p>
 * To store objects in bit set, the subclasses need to take care of
 * the mappings between objects and indexes by implementing {@link #getIndex}
 * and {@link #getElement}. The objects stored in the same bit set {@code s}
 * should preserve the invariant:
 * <code>e.equals(s.getElement(s.getIndex(e)))</code>.
 * <p>
 * Note: objects in different contexts may be mapped to the same index, and
 * it may cause unexpected behaviors if they are stored in the same bit set.
 * To avoid this, for each bit set, we require the subclasses to provide
 * a context object which indicates the context of the objects represented
 * by the bits in the set. We consider set operations (e.g., {@link #addAll}
 * and {@link #containsAll}) meaningful only when the two bit sets have
 * the equivalent context object.
 * <p>
 * TODO: add mod count
 *
 * @param <E> type of elements
 */
public abstract class GenericBitSet<E> extends AbstractSetEx<E>
        implements Serializable {

    protected IBitSet bitSet;

    protected GenericBitSet(boolean isSparse) {
        bitSet = IBitSet.newBitSet(isSparse);
    }

    @Override
    public boolean contains(Object o) {
        checkInvariant(o);
        return bitSet.get(getIndex((E) o));
    }

    @Override
    public boolean add(E e) {
        checkInvariant(e);
        return bitSet.set(getIndex(e));
    }

    @Override
    public boolean remove(Object o) {
        checkInvariant(o);
        return bitSet.clear(getIndex((E) o));
    }

    /**
     * The objects passed to this set should preserve this invariant.
     */
    private void checkInvariant(Object o) {
        assert o.equals(getElement(getIndex((E) o)));
    }

    @Override
    public boolean containsAll(@Nonnull Collection<?> c) {
        if (c instanceof GenericBitSet s) {
            checkContext(s);
            return bitSet.contains(s.bitSet);
        } else {
            return super.containsAll(c);
        }
    }

    @Override
    public boolean addAll(@Nonnull Collection<? extends E> c) {
        if (c instanceof GenericBitSet s) {
            checkContext(s);
            return bitSet.or(s.bitSet);
        } else {
            return super.addAll(c);
        }
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (c instanceof GenericBitSet s) {
            checkContext(s);
            return bitSet.andNot(s.bitSet);
        } else {
            return super.removeAll(c);
        }
    }

    @Override
    public boolean retainAll(@Nonnull Collection<?> c) {
        if (c instanceof GenericBitSet s) {
            checkContext(s);
            return bitSet.and(s.bitSet);
        } else {
            return super.retainAll(c);
        }
    }

    /**
     * Sets the content of this bit set to the same as given collection.
     */
    public void setTo(@Nonnull Collection<E> c) {
        if (c instanceof GenericBitSet s) {
            checkContext(s);
            bitSet.setTo(s.bitSet);
        } else {
            clear();
            addAll(c);
        }
    }

    /**
     * Checks if the set to operate on has equivalent context as this bit set.
     *
     * @param set the set to operate on
     */
    private void checkContext(GenericBitSet<?> set) {
        assert getContext().equals(set.getContext());
    }

    @Override
    public boolean isEmpty() {
        return bitSet.isEmpty();
    }

    @Override
    public void clear() {
        bitSet.clear();
    }

    @Override
    public int size() {
        return bitSet.cardinality();
    }

    @Override
    public Iterator<E> iterator() {
        return new BitSetIterator();
    }

    private class BitSetIterator implements Iterator<E> {

        /**
         * Current bit index of a set bit.
         */
        private int index;

        /**
         * Index of last iterated set bit; -1 if no such
         */
        private int lastRet;

        private BitSetIterator() {
            index = bitSet.nextSetBit(0);
            lastRet = -1;
        }

        @Override
        public boolean hasNext() {
            return index != -1;
        }

        @Override
        public E next() {
            int i = index;
            if (i == -1) {
                throw new NoSuchElementException();
            }
            index = bitSet.nextSetBit(i + 1); // advance
            return getElement(lastRet = i);
        }

        @Override
        public void remove() {
            if (lastRet < 0) {
                throw new IllegalStateException();
            }
            bitSet.clear(lastRet);
            lastRet = -1;
        }
    }

    @Override
    public int hashCode() {
        return Hashes.hash(getContext(), bitSet);
    }

    @Override
    public SetEx<E> copy() {
        GenericBitSet<E> copy = newSet();
        copy.bitSet = bitSet.copy();
        return copy;
    }

    @Override
    public SetEx<E> addAllDiff(Collection<? extends E> c) {
        if (c instanceof GenericBitSet s) {
            checkContext(s);
            GenericBitSet<E> diff = newSet();
            diff.bitSet = bitSet.orDiff(s.bitSet);
            return diff;
        } else {
            return super.addAllDiff(c);
        }
    }

    @Override
    protected abstract GenericBitSet<E> newSet();

    @Override
    public boolean hasOverlapWith(Set<E> other) {
        if (other instanceof GenericBitSet s) {
            checkContext(s);
            return bitSet.intersects(s.bitSet);
        } else {
            return super.hasOverlapWith(other);
        }
    }

    /**
     * @return the context for the objects represented by the bits in this set.
     */
    protected abstract Object getContext();

    /**
     * Maps an object to the corresponding index.
     */
    protected abstract int getIndex(E o);

    /**
     * Maps an index to the corresponding object.
     */
    protected abstract E getElement(int index);
}
