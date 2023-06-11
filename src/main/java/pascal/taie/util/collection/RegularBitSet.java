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

import java.io.Serializable;
import java.util.Arrays;

/**
 * Regular bit set implementation.
 * This implementation is very similar to {@link java.util.Set} which uses
 * a {@code long[]} to store all the set bits.
 */
public class RegularBitSet extends AbstractBitSet
        implements Serializable {

    /* Used to shift left or right for a partial word mask */
    private static final long WORD_MASK = 0xffffffffffffffffL;

    /**
     * The internal field corresponding to the serialField "bits".
     */
    private long[] words;

    /**
     * The number of words in the logical size of this BitSet.
     */
    private int wordsInUse = 0;

    /**
     * Creates a new bit set. All bits are initially {@code false}.
     */
    public RegularBitSet() {
        initWords(BITS_PER_WORD);
    }

    /**
     * Creates a bit set whose initial size is large enough to explicitly
     * represent bits with indices in the range {@code 0} through
     * {@code nbits-1}. All bits are initially {@code false}.
     *
     * @param nbits the initial size of the bit set
     * @throws NegativeArraySizeException if the specified initial size
     *                                    is negative
     */
    public RegularBitSet(int nbits) {
        // nbits can't be negative; size 0 is OK
        if (nbits < 0) {
            throw new NegativeArraySizeException("nbits < 0: " + nbits);
        }

        initWords(nbits);
    }

    private void initWords(int nbits) {
        words = new long[wordIndex(nbits - 1) + 1];
    }

    /**
     * Every public method must preserve these invariants.
     */
    private void checkInvariants() {
        assert (wordsInUse == 0 || words[wordsInUse - 1] != 0);
        assert (wordsInUse >= 0 && wordsInUse <= words.length);
        assert (wordsInUse == words.length || words[wordsInUse] == 0);
    }

    /**
     * Sets the field wordsInUse to the logical size in words of the bit set.
     * WARNING:This method assumes that the number of words actually in use is
     * less than or equal to the current value of wordsInUse!
     */
    private void recalculateWordsInUse() {
        // Traverse the bitset until a used word is found
        int i;
        for (i = wordsInUse - 1; i >= 0; i--) {
            if (words[i] != 0) {
                break;
            }
        }

        wordsInUse = i + 1; // The new logical size
    }

    /**
     * Ensures that the BitSet can hold enough words.
     *
     * @param wordsRequired the minimum acceptable number of words.
     */
    private void ensureCapacity(int wordsRequired) {
        if (words.length < wordsRequired) {
            // Allocate larger of doubled size or required size
            int request = Math.max(2 * words.length, wordsRequired);
            words = Arrays.copyOf(words, request);
        }
    }

    /**
     * Ensures that the BitSet can accommodate a given wordIndex,
     * temporarily violating the invariants.  The caller must
     * restore the invariants before returning to the user,
     * possibly using recalculateWordsInUse().
     *
     * @param wordIndex the index to be accommodated.
     */
    private void expandTo(int wordIndex) {
        int wordsRequired = wordIndex + 1;
        if (wordsInUse < wordsRequired) {
            ensureCapacity(wordsRequired);
            wordsInUse = wordsRequired;
        }
    }

    @Override
    public boolean set(int bitIndex) {
        if (bitIndex < 0) {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
        }

        int wordIndex = wordIndex(bitIndex);
        expandTo(wordIndex);

        long oldWord = words[wordIndex];
        long newWord = oldWord | (1L << bitIndex); // Restores invariants
        words[wordIndex] = newWord;

        checkInvariants();

        return oldWord != newWord;
    }

    @Override
    public boolean clear(int bitIndex) {
        if (bitIndex < 0) {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
        }

        int wordIndex = wordIndex(bitIndex);
        if (wordIndex >= wordsInUse) {
            return false;
        }

        long oldWord = words[wordIndex];
        long newWord = oldWord & ~(1L << bitIndex);
        words[wordIndex] = newWord;

        recalculateWordsInUse();
        checkInvariants();
        return oldWord != newWord;
    }

    @Override
    public boolean get(int bitIndex) {
        if (bitIndex < 0) {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
        }

        checkInvariants();

        int wordIndex = wordIndex(bitIndex);
        return (wordIndex < wordsInUse)
                && ((words[wordIndex] & (1L << bitIndex)) != 0);
    }

    @Override
    public void flip(int bitIndex) {
        if (bitIndex < 0) {
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
        }

        int wordIndex = wordIndex(bitIndex);
        expandTo(wordIndex);

        words[wordIndex] ^= (1L << bitIndex);

        recalculateWordsInUse();
        checkInvariants();
    }

    @Override
    public int nextSetBit(int fromIndex) {
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        }

        checkInvariants();

        int u = wordIndex(fromIndex);
        if (u >= wordsInUse) {
            return -1;
        }

        long word = words[u] & (WORD_MASK << fromIndex);

        while (true) {
            if (word != 0) {
                return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            }
            if (++u == wordsInUse) {
                return -1;
            }
            word = words[u];
        }
    }

    @Override
    public int nextClearBit(int fromIndex) {
        // Neither spec nor implementation handle bitsets of maximal length.
        // See 4816253.
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        }

        checkInvariants();

        int u = wordIndex(fromIndex);
        if (u >= wordsInUse) {
            return fromIndex;
        }

        long word = ~words[u] & (WORD_MASK << fromIndex);

        while (true) {
            if (word != 0) {
                return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            }
            if (++u == wordsInUse) {
                return wordsInUse * BITS_PER_WORD;
            }
            word = ~words[u];
        }
    }

    @Override
    public int previousSetBit(int fromIndex) {
        if (fromIndex < 0) {
            if (fromIndex == -1) {
                return -1;
            }
            throw new IndexOutOfBoundsException(
                    "fromIndex < -1: " + fromIndex);
        }

        checkInvariants();

        int u = wordIndex(fromIndex);
        if (u >= wordsInUse) {
            return length() - 1;
        }

        long word = words[u] & (WORD_MASK >>> -(fromIndex + 1));

        while (true) {
            if (word != 0) {
                return (u + 1) * BITS_PER_WORD - 1 - Long.numberOfLeadingZeros(word);
            }
            if (u-- == 0) {
                return -1;
            }
            word = words[u];
        }
    }

    @Override
    public int previousClearBit(int fromIndex) {
        if (fromIndex < 0) {
            if (fromIndex == -1) {
                return -1;
            }
            throw new IndexOutOfBoundsException(
                    "fromIndex < -1: " + fromIndex);
        }

        checkInvariants();

        int u = wordIndex(fromIndex);
        if (u >= wordsInUse) {
            return fromIndex;
        }

        long word = ~words[u] & (WORD_MASK >>> -(fromIndex + 1));

        while (true) {
            if (word != 0) {
                return (u + 1) * BITS_PER_WORD - 1 - Long.numberOfLeadingZeros(word);
            }
            if (u-- == 0) {
                return -1;
            }
            word = ~words[u];
        }
    }

    @Override
    public boolean intersects(IBitSet set) {
        if (this == set) {
            return true;
        }
        if (!(set instanceof RegularBitSet other)) {
            return super.intersects(set);
        }
        for (int i = Math.min(wordsInUse, other.wordsInUse) - 1; i >= 0; i--) {
            if ((words[i] & other.words[i]) != 0) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean contains(IBitSet set) {
        if (this == set) {
            return true;
        }

        if (!(set instanceof RegularBitSet other)) {
            return super.contains(set);
        }

        if (wordsInUse < other.wordsInUse) {
            // set uses more words, so it must contain some bit(s) that
            // are not in this set
            return false;
        }
        int wordsInCommon = other.wordsInUse;
        for (int i = 0; i < wordsInCommon; ++i) {
            long word = words[i];
            if ((word | other.words[i]) != word) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean and(IBitSet set) {
        if (this == set) {
            return false;
        }
        if (!(set instanceof RegularBitSet other)) {
            throw new UnsupportedOperationException(
                    String.format("%s does not support AND with %s",
                            this.getClass(), set.getClass()));
        }

        boolean changed = false;
        if (wordsInUse > other.wordsInUse) {
            Arrays.fill(words, other.wordsInUse, wordsInUse, 0);
            wordsInUse = other.wordsInUse;
            changed = true;
        }

        // Perform logical AND on words in common
        for (int i = 0; i < wordsInUse; i++) {
            if (changed) {
                // already know set changed, just perform logical AND
                words[i] &= other.words[i];
            } else {
                long oldWord = words[i];
                long newWord = oldWord & other.words[i];
                if (oldWord != newWord) {
                    words[i] = newWord;
                    changed = true;
                }
            }
        }

        recalculateWordsInUse();
        checkInvariants();
        return changed;
    }

    @Override
    public boolean andNot(IBitSet set) {
        boolean changed = false;
        if (this == set) {
            changed = !isEmpty();
            clear();
            return changed;
        }
        if (!(set instanceof RegularBitSet other)) {
            return super.andNot(set);
        }

        // Perform logical (a & !b) on words in common
        int wordsInCommon = Math.min(wordsInUse, other.wordsInUse);
        for (int i = wordsInCommon - 1; i >= 0; i--) {
            if (changed) {
                words[i] &= ~other.words[i];
            } else {
                long oldWord = words[i];
                long newWord = oldWord & ~other.words[i];
                if (oldWord != newWord) {
                    words[i] = newWord;
                    changed = true;
                }
            }
        }

        recalculateWordsInUse();
        checkInvariants();
        return changed;
    }

    @Override
    public boolean or(IBitSet set) {
        if (this == set) {
            return false;
        }
        if (!(set instanceof RegularBitSet other)) {
            return super.or(set);
        }

        int wordsInCommon = Math.min(wordsInUse, other.wordsInUse);

        boolean changed = false;
        if (wordsInUse < other.wordsInUse) {
            ensureCapacity(other.wordsInUse);
            wordsInUse = other.wordsInUse;
            changed = true;
        }

        // Perform logical OR on words in common
        for (int i = 0; i < wordsInCommon; i++) {
            if (changed) {
                // already know set changed, just perform logical OR
                words[i] |= other.words[i];
            } else {
                long oldWord = words[i];
                long newWord = oldWord | other.words[i];
                if (oldWord != newWord) {
                    words[i] = newWord;
                    changed = true;
                }
            }
        }

        // Copy any remaining words
        if (wordsInCommon < other.wordsInUse) {
            System.arraycopy(other.words, wordsInCommon,
                    words, wordsInCommon,
                    wordsInUse - wordsInCommon);
        }

        // recalculateWordsInUse() is unnecessary
        checkInvariants();
        return changed;
    }

    @Override
    public IBitSet orDiff(IBitSet set) {
        RegularBitSet diff = new RegularBitSet();
        if (this == set) {
            return diff;
        }
        if (!(set instanceof RegularBitSet other)) {
            return super.orDiff(set);
        }
        if (wordsInUse < other.wordsInUse) {
            ensureCapacity(other.wordsInUse);
            wordsInUse = other.wordsInUse;
        }
        boolean foundNonZero = false;
        for (int i = other.wordsInUse - 1; i >= 0; --i) {
            long oldWord = words[i];
            long otherWord = other.words[i];
            words[i] = oldWord | otherWord;
            // compute diff
            long diffWord = otherWord & ~oldWord;
            if (diffWord != 0) {
                if (!foundNonZero) {
                    diff.ensureCapacity(i + 1);
                    diff.wordsInUse = i + 1;
                    foundNonZero = true;
                }
                diff.words[i] = diffWord;
            }
        }
        return diff;
    }

    @Override
    public boolean xor(IBitSet set) {
        boolean changed = false;
        if (this == set) {
            changed = !isEmpty();
            clear();
            return changed;
        }
        if (!(set instanceof RegularBitSet other)) {
            return super.xor(set);
        }

        int wordsInCommon = Math.min(wordsInUse, other.wordsInUse);

        if (wordsInUse < other.wordsInUse) {
            ensureCapacity(other.wordsInUse);
            wordsInUse = other.wordsInUse;
            changed = true;
        }

        // Perform logical XOR on words in common
        for (int i = 0; i < wordsInCommon; i++) {
            if (changed) {
                // already know set changed, just perform logical XOR
                words[i] ^= other.words[i];
            } else {
                long oldWord = words[i];
                long newWord = oldWord ^ other.words[i];
                if (oldWord != newWord) {
                    words[i] = newWord;
                    changed = true;
                }
            }
        }

        // Copy any remaining words
        if (wordsInCommon < other.wordsInUse) {
            System.arraycopy(other.words, wordsInCommon,
                    words, wordsInCommon,
                    other.wordsInUse - wordsInCommon);
        }

        recalculateWordsInUse();
        checkInvariants();
        return changed;
    }

    @Override
    public void setTo(IBitSet set) {
        if (this == set) {
            return;
        }

        if (!(set instanceof RegularBitSet other)) {
            super.setTo(set);
            return;
        }

        if (words.length < other.wordsInUse) {
            words = Arrays.copyOf(other.words, other.wordsInUse);
        } else {
            System.arraycopy(other.words, 0, words, 0, other.wordsInUse);
            if (other.wordsInUse < wordsInUse) {
                Arrays.fill(words, other.wordsInUse, wordsInUse, 0);
            }
        }
        wordsInUse = other.wordsInUse;
    }

    @Override
    public void clear() {
        Arrays.fill(words, 0, wordsInUse, 0);
        wordsInUse = 0;
    }

    @Override
    public boolean isEmpty() {
        return wordsInUse == 0;
    }

    @Override
    public int length() {
        if (wordsInUse == 0) {
            return 0;
        }

        return BITS_PER_WORD * (wordsInUse - 1) +
                (BITS_PER_WORD - Long.numberOfLeadingZeros(words[wordsInUse - 1]));
    }

    @Override
    public int size() {
        return words.length * BITS_PER_WORD;
    }

    @Override
    public int cardinality() {
        int sum = 0;
        for (int i = 0; i < wordsInUse; i++) {
            sum += Long.bitCount(words[i]);
        }
        return sum;
    }

    /**
     * Returns the hash code value for this bit set. The hash code depends
     * only on which bits are set within this {@code BitSet}.
     *
     * <p>The hash code is defined to be the result of the following
     * calculation:
     * <pre> {@code
     * public int hashCode() {
     *     long h = 1234;
     *     long[] words = toLongArray();
     *     for (int i = words.length; --i >= 0; )
     *         h ^= words[i] * (i + 1);
     *     return (int)((h >> 32) ^ h);
     * }}</pre>
     * Note that the hash code changes if the set of bits is altered.
     *
     * @return the hash code value for this bit set
     */
    @Override
    public int hashCode() {
        long h = 1234;
        for (int i = wordsInUse; --i >= 0; ) {
            h ^= words[i] * (i + 1);
        }

        return (int) ((h >> 32) ^ h);
    }

    /**
     * Compares this object against the specified object.
     * The result is {@code true} if and only if the argument is
     * not {@code null} and is a {@code BitSet} object that has
     * exactly the same set of bits set to {@code true} as this bit
     * set. That is, for every nonnegative {@code int} index {@code k},
     * <pre>((BitSet)obj).get(k) == this.get(k)</pre>
     * must be true. The current sizes of the two bit sets are not compared.
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are the same;
     * {@code false} otherwise
     * @see #size()
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RegularBitSet set)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        checkInvariants();
        set.checkInvariants();

        if (wordsInUse != set.wordsInUse) {
            return false;
        }

        // Check words in use by both BitSets
        for (int i = 0; i < wordsInUse; i++) {
            if (words[i] != set.words[i]) {
                return false;
            }
        }

        return true;
    }

    @Override
    public RegularBitSet copy() {
        RegularBitSet copy = new RegularBitSet();
        copy.wordsInUse = wordsInUse;
        copy.words = Arrays.copyOf(words, wordsInUse);
        return copy;
    }
}
