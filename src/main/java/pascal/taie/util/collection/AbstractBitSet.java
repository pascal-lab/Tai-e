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

import pascal.taie.util.Hashes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Bit set based implementation of {@link java.util.Set}.
 *
 * The subclasses just need to take care of the mappings between indexes
 * and objects.
 *
 * @param <E> type of elements
 */
public abstract class AbstractBitSet<E> extends AbstractSet<E> {

    protected final BitSet bitSet;

    protected AbstractBitSet() {
        bitSet = new BitSet();
    }

    protected AbstractBitSet(AbstractBitSet<E> s) {
        bitSet = new BitSet(s.bitSet);
    }

    @Override
    public boolean contains(Object o) {
        return bitSet.get(getIndex((E) o));
    }

    @Override
    public boolean add(E e) {
        return bitSet.set(getIndex(e));
    }

    @Override
    public boolean remove(Object o) {
        return bitSet.clear(getIndex((E) o));
    }

    @Override
    public boolean containsAll(@Nonnull Collection<?> c) {
        if (c instanceof AbstractBitSet s) {
            return Objects.equals(getContext(), s.getContext()) &&
                    bitSet.contains(s.bitSet);
        } else {
            return super.containsAll(c);
        }
    }

    @Override
    public boolean addAll(@Nonnull Collection<? extends E> c) {
        if (c instanceof AbstractBitSet s) {
            return Objects.equals(getContext(), s.getContext()) &&
                    bitSet.or(s.bitSet);
        } else {
            return super.addAll(c);
        }
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (c instanceof AbstractBitSet s) {
            return Objects.equals(getContext(), s.getContext()) &&
                    bitSet.andNot(s.bitSet);
        } else {
            return super.removeAll(c);
        }
    }

    @Override
    public boolean retainAll(@Nonnull Collection<?> c) {
        if (c instanceof AbstractBitSet s) {
            return Objects.equals(getContext(), s.getContext()) &&
                    bitSet.and(s.bitSet);
        } else {
            return super.retainAll(c);
        }
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

        private BitSetIterator() {
            index = bitSet.nextSetBit(0);
        }

        @Override
        public boolean hasNext() {
            return index != -1;
        }

        @Override
        public E next() {
            if (index == -1) {
                throw new NoSuchElementException();
            }
            E ret = getElement(index);
            index = bitSet.nextSetBit(index); // advance
            return ret;
        }

        @Override
        public void remove() {
            bitSet.clear(index);
        }
    }

    @Override
    public int hashCode() {
        return Hashes.safeHash(getContext(), bitSet);
    }

    /**
     * Creates a copy of this set.
     */
    public abstract AbstractBitSet<E> copy();

    /**
     * @return the context object of the elements represented by
     * the bits in {@link #bitSet}. This ... useful for two ...
     * typically, the container of the elements
     */
    protected abstract @Nullable Object getContext();

    /**
     * Maps an object to the corresponding index.
     * @throws IllegalArgumentException if given object cannot be properly
     * mapped to an index.
     */
    protected abstract int getIndex(E o) throws IllegalArgumentException;

    /**
     * Maps an index to the corresponding object.
     * @throws IllegalArgumentException if given index cannot be properly
     * mapped to an index.
     */
    protected abstract E getElement(int index) throws IllegalArgumentException;
}
