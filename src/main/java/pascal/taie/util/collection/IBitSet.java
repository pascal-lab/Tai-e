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

import pascal.taie.util.Copyable;

import java.io.Serializable;

/**
 * Interface for different bit set implementations.
 * <p>
 * This interface is similar to {@link java.util.Set}. The main motivation
 * to reinvent a bit set is that the APIs of {@link java.util.Set}
 * do not fulfill the requirements of program analysis.
 * <p>
 * For APIs that may modify a bit set, such {@link #set(int)},
 * {@link #and(IBitSet)}, and {@link #or(IBitSet)}, this implementation
 * returns whether the bit set changed. In addition, it provides some
 * useful operations that are absent in {@link java.util.Set}.
 */
public interface IBitSet extends Copyable<IBitSet>, Serializable {

    // ------------------------------------------------------------------------
    // single-bit operations
    // ------------------------------------------------------------------------

    /**
     * Sets the bit at the specified index to {@code true}.
     *
     * @param bitIndex a bit index
     * @return {@code true} if this BitSet changed as a result of the call
     * @throws IndexOutOfBoundsException if the specified index is negative
     */
    boolean set(int bitIndex);

    /**
     * Sets the bit at the specified index to the specified value.
     *
     * @param bitIndex a bit index
     * @param value    a boolean value to set
     * @return {@code true} if this BitSet changed as a result of the call
     * @throws IndexOutOfBoundsException if the specified index is negative
     */
    boolean set(int bitIndex, boolean value);

    /**
     * Sets the bit specified by the index to {@code false}.
     *
     * @param bitIndex the index of the bit to be cleared
     * @return {@code true} if this BitSet changed as a result of the call
     * @throws IndexOutOfBoundsException if the specified index is negative
     */
    boolean clear(int bitIndex);

    /**
     * Returns the value of the bit with the specified index. The value
     * is {@code true} if the bit with the index {@code bitIndex}
     * is currently set in this {@code BitSet}; otherwise, the result
     * is {@code false}.
     *
     * @param bitIndex the bit index
     * @return the value of the bit with the specified index
     * @throws IndexOutOfBoundsException if the specified index is negative
     */
    boolean get(int bitIndex);

    /**
     * Sets the bit at the specified index to the complement of its
     * current value. This operation must modify the BitSet.
     *
     * @param bitIndex the index of the bit to flip
     * @throws IndexOutOfBoundsException if the specified index is negative
     */
    void flip(int bitIndex);

    /**
     * Returns the index of the first bit that is set to {@code true}
     * that occurs on or after the specified starting index. If no such
     * bit exists then {@code -1} is returned.
     *
     * <p>To iterate over the {@code true} bits in a {@code BitSet},
     * use the following loop:
     *
     * <pre> {@code
     * for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1)) {
     *     // operate on index i here
     *     if (i == Integer.MAX_VALUE) {
     *         break; // or (i+1) would overflow
     *     }
     * }}</pre>
     *
     * @param fromIndex the index to start checking from (inclusive)
     * @return the index of the next set bit, or {@code -1} if there
     * is no such bit
     * @throws IndexOutOfBoundsException if the specified index is negative
     */
    int nextSetBit(int fromIndex);

    /**
     * Returns the index of the first bit that is set to {@code false}
     * that occurs on or after the specified starting index.
     *
     * @param fromIndex the index to start checking from (inclusive)
     * @return the index of the next clear bit
     * @throws IndexOutOfBoundsException if the specified index is negative
     */
    int nextClearBit(int fromIndex);

    /**
     * Returns the index of the nearest bit that is set to {@code true}
     * that occurs on or before the specified starting index.
     * If no such bit exists, or if {@code -1} is given as the
     * starting index, then {@code -1} is returned.
     *
     * <p>To iterate over the {@code true} bits in a {@code BitSet},
     * use the following loop:
     *
     * <pre> {@code
     * for (int i = bs.length(); (i = bs.previousSetBit(i-1)) >= 0; ) {
     *     // operate on index i here
     * }}</pre>
     *
     * @param fromIndex the index to start checking from (inclusive)
     * @return the index of the previous set bit, or {@code -1} if there
     * is no such bit
     * @throws IndexOutOfBoundsException if the specified index is less
     *                                   than {@code -1}
     */
    int previousSetBit(int fromIndex);

    /**
     * Returns the index of the nearest bit that is set to {@code false}
     * that occurs on or before the specified starting index.
     * If no such bit exists, or if {@code -1} is given as the
     * starting index, then {@code -1} is returned.
     *
     * @param fromIndex the index to start checking from (inclusive)
     * @return the index of the previous clear bit, or {@code -1} if there
     * is no such bit
     * @throws IndexOutOfBoundsException if the specified index is less
     *                                   than {@code -1}
     */
    int previousClearBit(int fromIndex);

    // ------------------------------------------------------------------------
    // bulk operations
    // ------------------------------------------------------------------------

    /**
     * Returns {@code true} if the specified {@code BitSet} has any bits set to
     * {@code true} that are also set to {@code true} in this {@code BitSet}.
     *
     * @param set {@code BitSet} to intersect with
     * @return boolean indicating whether this {@code BitSet} intersects
     * the specified {@code BitSet}
     */
    boolean intersects(IBitSet set);

    /**
     * Returns {@code false} if the specified {@code BitSet} has any bits set to
     * {@code true} that are also set to {@code true} in this {@code BitSet}.
     *
     * @param set {@code BitSet} to disjoint with
     * @return boolean indicating whether this {@code BitSet} disjoints
     * the specified {@code BitSet}
     */
    boolean disjoints(IBitSet set);

    /**
     * Returns {@code true} if this {@code BitSet} contains all set bits in
     * the specified {@code BitSet}.
     *
     * @param set bit set to be checked for containment in this set.
     * @return boolean indicating whether this {@code BitSet} contains all
     * set bits in the specified {@code BitSet}
     */
    boolean contains(IBitSet set);

    /**
     * Performs a logical <b>AND</b> of this target bit set with the
     * argument bit set. This bit set is modified so that each bit in it
     * has the value {@code true} if and only if it both initially
     * had the value {@code true} and the corresponding bit in the
     * bit set argument also had the value {@code true}.
     *
     * @param set a bit set
     * @return {@code true} if this bit set changed as a result of the call
     */
    boolean and(IBitSet set);

    /**
     * Clears all of the bits in this {@code BitSet} whose corresponding
     * bit is set in the specified {@code BitSet}.
     *
     * @param set the {@code BitSet} with which to mask this {@code BitSet}
     * @return {@code true} if this bit set changed as a result of the call
     */
    boolean andNot(IBitSet set);

    /**
     * Performs a logical <b>OR</b> of this bit set with the bit set
     * argument. This bit set is modified so that a bit in it has the
     * value {@code true} if and only if it either already had the
     * value {@code true} or the corresponding bit in the bit set
     * argument has the value {@code true}.
     *
     * @param set a bit set
     * @return {@code true} if this bit set changed as a result of the call
     */
    boolean or(IBitSet set);

    /**
     * Performs a logical <b>OR</b> of this bit set with the bit set argument,
     * computes and returns the difference set between given bit set and
     * this set (before performing logical <b>OR</b>).
     *
     * @param set a bit set.
     * @return a new bit set of bit values that are present in the bit set
     * argument and were absent in this bit set before.
     */
    IBitSet orDiff(IBitSet set);

    /**
     * Performs a logical <b>XOR</b> of this bit set with the bit set
     * argument. This bit set is modified so that a bit in it has the
     * value {@code true} if and only if one of the following
     * statements holds:
     * <ul>
     * <li>The bit initially has the value {@code true}, and the
     *     corresponding bit in the argument has the value {@code false}.
     * <li>The bit initially has the value {@code false}, and the
     *     corresponding bit in the argument has the value {@code true}.
     * </ul>
     *
     * @param set a bit set
     * @return {@code true} if this bit set changed as a result of the call
     */
    boolean xor(IBitSet set);

    /**
     * Sets the content of this bit set to the same as specified bit set.
     *
     * @param set a bit set
     */
    void setTo(IBitSet set);

    /**
     * Sets all of the bits in this BitSet to {@code false}.
     */
    void clear();

    /**
     * Iterates all set bits in this set and takes action on them.
     */
    default <R> R iterateBits(Action<R> action) {
        int i = nextSetBit(0);
        while (i != -1) {
            if (!action.accept(i)) {
                break;
            }
            i = nextSetBit(i + 1);
        }
        return action.getResult();
    }

    /**
     * Action on set bits.
     *
     * @param <R> type of final result of the action
     */
    interface Action<R> {

        /**
         * Performs this action on given bit index.
         *
         * @param bitIndex the input bit index.
         * @return {@code true} if the iteration should keep going after
         * processing {@code bitIndex}.
         */
        boolean accept(int bitIndex);

        /**
         * @return the final result of the iteration.
         */
        R getResult();
    }

    // ------------------------------------------------------------------------
    // state queries
    // ------------------------------------------------------------------------

    /**
     * Returns true if this {@code BitSet} contains no bits that are set
     * to {@code true}.
     *
     * @return boolean indicating whether this {@code BitSet} is empty
     */
    boolean isEmpty();

    /**
     * Returns the "logical size" of this {@code BitSet}: the index of
     * the highest set bit in the {@code BitSet} plus one. Returns zero
     * if the {@code BitSet} contains no set bits.
     *
     * @return the logical size of this {@code BitSet}
     */
    int length();

    /**
     * Returns the number of bits of space actually in use by this
     * {@code BitSet} to represent bit values.
     * The maximum element in the set is the size - 1st element.
     *
     * @return the number of bits currently in this bit set
     */
    int size();

    /**
     * Returns the number of bits set to {@code true} in this {@code BitSet}.
     *
     * @return the number of bits set to {@code true} in this {@code BitSet}
     */
    int cardinality();

    // ------------------------------------------------------------------------
    // utilities
    // ------------------------------------------------------------------------

    /**
     * Creates a new set.
     */
    static IBitSet newBitSet(boolean isSparse) {
        return isSparse ? new SparseBitSet() : new RegularBitSet();
    }

    /**
     * @return {@code true} if the given bit set is sparse.
     */
    static boolean isSparse(IBitSet set) {
        return set instanceof SparseBitSet;
    }

    /**
     * Creates a bit set that contains given bits.
     */
    static IBitSet of(int... bits) {
        IBitSet result = newBitSet(false);
        for (int i : bits) {
            result.set(i);
        }
        return result;
    }
}
