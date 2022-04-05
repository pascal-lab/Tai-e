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

import javax.annotation.Nullable;

public class SparseBitSet extends AbstractBitSet {

    //==============================================================================
    //  The critical parameters. These are set up so that the compiler may
    //  pre-compute all the values as compile-time constants.
    //==============================================================================

    /**
     *  The number of bits in a positive integer, and the size of permitted index
     *  of a bit in the bit set.
     */
    private static final int INDEX_SIZE = Integer.SIZE - 1;

    /**
     *  The number of bits in a long value.
     */
    private static final int LENGTH4 = Long.SIZE;

    /**
     *  The label (index) of a bit in the bit set is essentially broken into
     *  4 "levels". Respectively (from the least significant end), level4, the
     *  address within word, the address within a level3 block, the address within
     *  a level2 area, and the level1 address of that area within the set.
     *
     *  LEVEL4 is the number of bits of the level4 address (number of bits need
     *  to address the bits in a long)
     */
    private static final int LEVEL4 = 6;

    /**
     *  LEVEL3 is the number of bits of the level3 address.
     */
    private static final int LEVEL3 = 5; // Do not change!

    /**
     *  LEVEL2 is the number of bits of the level2 address.
     */
    private static final int LEVEL2 = 5; // Do not change!

    /**
     *  LEVEL1 is the number of bits of the level1 address.
     */
    private static final int LEVEL1 = INDEX_SIZE - LEVEL2 - LEVEL3 - LEVEL4;

    /**
     *  MAX_LENGTH1 is the maximum number of entries in the level1 set array.
     */
    private static final int MAX_LENGTH1 = 1 << LEVEL1;

    /**
     *  LENGTH2 is the number of entries in the any level2 area.
     */
    private static final int LENGTH2 = 1 << LEVEL2;

    /**
     *  LENGTH3 is the number of entries in the any level3 block.
     */
    private static final int LENGTH3 = 1 << LEVEL3;

    /**
     *  The shift to create the word index. (I.e., move it to the right end)
     */
    private static final int SHIFT3 = LEVEL4;

    /**
     *  MASK3 is the mask to extract the LEVEL3 address from a word index
     *  (after shifting by SHIFT3).
     */
    private static final int MASK3 = LENGTH3 - 1;

    /**
     *  SHIFT2 is the shift to bring the level2 address (from the word index) to
     *  the right end (i.e., after shifting by SHIFT3).
     */
    private static final int SHIFT2 = LEVEL3;

    /**
     *  MASK2 is the mask to extract the LEVEL2 address from a word index
     *  (after shifting by SHIFT3 and SHIFT2).
     */
    private static final int MASK2 = LENGTH2 - 1;

    /**
     *  SHIFT1 is the shift to bring the level1 address (from the word index) to
     *  the right end (i.e., after shifting by SHIFT3).
     */
    private static final int SHIFT1 = LEVEL2 + LEVEL3;

    /**
     *  UNIT is the greatest number of bits that can be held in one level1 entry.
     *  That is, bits per word by words per level3 block by blocks per level2 area.
     */
    private static final int UNIT = LENGTH2 * LENGTH3 * LENGTH4;

    /**
     *  LENGTH4_SIZE is maximum index of a bit in a LEVEL4 word.
     */
    private static final int LENGTH4_SIZE = LENGTH4 - 1;

    /**
     *  LENGTH3_SIZE is maximum index of a LEVEL3 page.
     */
    private static final int LENGTH3_SIZE = LENGTH3 - 1;

    /**
     *  LENGTH2_SIZE is maximum index of a LEVEL2 page.
     */
    private static final int LENGTH2_SIZE = LENGTH2 - 1;

    /** An empty level 3 block is kept for use when scanning. When a source block
     *  is needed, and there is not already one in the corresponding bit set, the
     *  ZERO_BLOCK is used (as a read-only block). It is a source of zero values
     *  so that code does not have to test for a null level3 block. This is a
     *  static block shared everywhere.
     */
    private static final long[] ZERO_BLOCK = new long[LENGTH3];

    // ------------------------------------------------------------------------
    // instance fields
    // ------------------------------------------------------------------------
    /**
     *  The storage for this SparseBitSet. The <i>i</i>th bit is stored in a word
     *  represented by a long value, and is at bit position <code>i % 64</code>
     *  within that word (where bit position 0 refers to the least significant bit
     *  and 63 refers to the most significant bit).
     *  <p>
     *  The words are organized into blocks, and the blocks are accessed by two
     *  additional levels of array indexing.
     */
    private transient long[][][] table;

    /**
     *  For the current size of the bits array, this is the maximum possible
     *  length of the bit set, i.e., the index of the last possible bit, plus one.
     *  Note: this not the value returned by <i>length</i>().
     * @see #resize(int)
     * @see #length()
     */
    private transient int bitsLength;

    /**
     *  A spare level 3 block is kept for use when scanning. When a target block
     *  is needed, and there is not already one in the bit set, the spare is
     *  provided. If non-zero values are placed into this block, it is moved to the
     *  resulting set, and a new spare is acquired. Note: a new spare needs to
     *  be allocated when the set is cloned (so that the spare is not shared
     *  between two sets).
     */
    private transient long[] spareBlock;

    private transient State state;

    public SparseBitSet() {
        this(1);
    }

    public SparseBitSet(int nbits) {
        if (nbits < 0) {
            throw new NegativeArraySizeException("nbits < 0: " + nbits);
        }
        resize(nbits - 1);
        spareBlock = new long[LENGTH3];
        state = new State();
        updateState();
    }

    @Override
    public boolean set(int bitIndex) {
        if ((bitIndex + 1) < 1) {
            throw new IndexOutOfBoundsException("bitIndex=" + bitIndex);
        }
        if (bitIndex >= bitsLength) {
            resize(bitIndex);
        }

        final int w = wordIndex(bitIndex);
        final int w1 = level1Index(w);
        long[][] a2;
        if ((a2 = table[w1]) == null) {
            a2 = table[w1] = new long[LENGTH2][];
        }
        final int w2 = level2Index(w);
        long[] a3;
        if ((a3 = a2[w2]) == null) {
            a3 = a2[w2] = new long[LENGTH3];
        }
        int w3 = level3Index(w);
        long oldWord = a3[w3];
        long newWord = oldWord | (1L << bitIndex);
        if (oldWord != newWord) {
            a3[w3] = newWord;
            invalidateState();
            return true;
        }
        return false;
    }

    @Override
    public boolean clear(int bitIndex) {
        if ((bitIndex + 1) < 1) {
            throw new IndexOutOfBoundsException("bitIndex=" + bitIndex);
        }
        if (bitIndex >= bitsLength) {
            return false;
        }
        final int w = wordIndex(bitIndex);
        long[][] a2;
        if ((a2 = table[level1Index(w)]) == null) {
            return false;
        }
        long[] a3;
        if ((a3 = a2[level2Index(w)]) == null) {
            return false;
        }
        int w3 = level3Index(w);
        long oldWord = a3[w3];
        long newWord = oldWord & ~(1L << bitIndex); //  Clear the indicated bit
        if (oldWord != newWord) {
            // In the interests of speed, no check is made here on whether the
            // level3 block goes to all zero. This may be found and corrected
            // in some later operation.
            a3[w3] = newWord;
            invalidateState();
            return true;
        }
        return true;
    }

    @Override
    public boolean get(int bitIndex) {
        if ((bitIndex + 1) < 1) {
            throw new IndexOutOfBoundsException("bitIndex=" + bitIndex);
        }
        final int w = wordIndex(bitIndex);
        long[][] a2;
        long[] a3;
        return bitIndex < bitsLength
                && (a2 = table[level1Index(w)]) != null
                && (a3 = a2[level2Index(w)]) != null
                && ((a3[level3Index(w)] & (1L << bitIndex)) != 0);
    }

    @Override
    public void flip(int bitIndex) {
        if ((bitIndex + 1) < 1) {
            throw new IndexOutOfBoundsException("bitIndex=" + bitIndex);
        }
        final int w = wordIndex(bitIndex);
        final int w1 = level1Index(w);
        final int w2 = level2Index(w);

        if (bitIndex >= bitsLength) {
            resize(bitIndex);
        }
        long[][] a2;
        if ((a2 = table[w1]) == null)
            a2 = table[w1] = new long[LENGTH2][];
        long[] a3;
        if ((a3 = a2[w2]) == null)
            a3 = a2[w2] = new long[LENGTH3];
        a3[level3Index(w)] ^= (1L << bitIndex); // Flip the designated bit
        invalidateState();
    }

    @Override
    public int nextSetBit(int fromIndex) {
        // The index value of this method is permitted to be Integer.MAX_VALUE,
        // as this is needed to make the loop defined above work: just in case the
        // bit labelled Integer.MAX_VALUE-1 is set. This case is not optimised:
        // but eventually -1 will be returned, as this will be included with
        // any search that goes off the end of the level1 array.
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        }
        // This is the word from which the search begins.
        int w = wordIndex(fromIndex);
        int w1 = level1Index(w);
        int w2 = level2Index(w);
        int w3 = level3Index(w);

        long word = 0L;
        final int tableLength = table.length;

        long[][] a2;
        long[] a3;
        // first check whether bitIndex itself (or the bits next to bitIndex
        // in the same word) has been set
        if (w1 < tableLength && ((a2 = table[w1]) == null
                || (a3 = a2[w2]) == null
                || ((word = a3[w3] & (~0L << fromIndex)) == 0L))) {
            // bitIndex is not set, start a search since bitIndex + 1
            ++w;
            w1 = level1Index(w);
            w2 = level2Index(w);
            w3 = level3Index(w);
            outer:
            for (; w1 != tableLength; ++w1) {
                if ((a2 = table[w1]) != null) {
                    for (; w2 != LENGTH2; ++w2) {
                        if ((a3 = a2[w2]) != null) {
                            for (; w3 != LENGTH3; ++w3) {
                                if ((word = a3[w3]) != 0) {
                                    break outer;
                                }
                            }
                        }
                        w3 = 0; // reset w3
                    }
                }
                w2 = w3 = 0; // reset w2 and w3
            }
        }
        return (w1 >= tableLength ? -1
                : bitIndex(w1, w2, w3) + Long.numberOfTrailingZeros(word));
    }

    @Override
    public int nextClearBit(int fromIndex) {
        // The index of this method is permitted to be Integer.MAX_VALUE,
        // as this is needed to make this method work together with the method
        // nextSetBit()--as might happen if a search for the next clear bit is
        // started after finding a set bit labelled Integer.MAX_VALUE-1. This
        // case is not optimised, the code will eventually return -1 (since
        // the Integer.MAX_VALUE-th bit does "exist," and is 0).

        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        }
        // This is the word from which the search begins.
        int w = wordIndex(fromIndex);
        int w1 = level1Index(w);
        int w2 = level2Index(w);
        int w3 = level3Index(w);

        long nword = (~0L << fromIndex);
        final int tableLength = table.length;

        long[][] a2;
        long[] a3;
        // first check whether bitIndex itself (or the bits next to bitIndex
        // in the same word) is clear (not set).
        if (w1 < tableLength && (a2 = table[w1]) != null
                && (a3 = a2[w2]) != null
                && ((nword = ~a3[w3] & (~0L << fromIndex))) == 0L) {
            // bitIndex is clear (not set), start a search since bitIndex + 1
            ++w;
            w1 = level1Index(w);
            w2 = level2Index(w);
            w3 = level3Index(w);
            nword = ~0L;
            outer:
            for (; w1 != tableLength; ++w1) {
                if ((a2 = table[w1]) == null) {
                    break;
                }
                for (; w2 != LENGTH2; ++w2) {
                    if ((a3 = a2[w2]) == null) {
                        break outer;
                    }
                    for (; w3 != LENGTH3; ++w3) {
                        if ((nword = ~a3[w3]) != 0) {
                            break outer;
                        }
                    }
                    w3 = 0;
                }
                w2 = w3 = 0;
            }
        }
        final int result = bitIndex(w1, w2, w3) + Long.numberOfTrailingZeros(nword);
        return (result == Integer.MAX_VALUE ? -1 : result);
    }

    @Override
    public int previousSetBit(int fromIndex) {
        if (fromIndex < 0) {
            if (fromIndex == -1) {
                return -1;
            }
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        }

        final long[][][] table = this.table;
        final int tableSize = table.length - 1;

        /*  This is the word from which the search begins. */
        final int w = wordIndex(fromIndex);
        int w1 = level1Index(w);
        int w2, w3, w4;
        if (w1 > tableSize) {
            // If the fromIndex is out of scope of table, then start from
            // the very end of the table.
            w1 = tableSize;
            w2 = LENGTH2_SIZE;
            w3 = LENGTH3_SIZE;
            w4 = LENGTH4_SIZE;
        } else {
            w2 = level2Index(w);
            w3 = level3Index(w);
            w4 = fromIndex % LENGTH4;
        }
        long word;
        long[][] a2;
        long[] a3;
        for (; w1 >= 0; --w1) {
            if ((a2 = table[w1]) != null) {
                for (; w2 >= 0; --w2) {
                    if ((a3 = a2[w2]) != null) {
                        for (; w3 >= 0; --w3) {
                            if ((word = a3[w3]) != 0) {
                                for (int offset = w4; offset >= 0; --offset) {
                                    if ((word & (1L << offset)) != 0) {
                                        return bitIndex(w1, w2, w3) + offset;
                                    }
                                }
                            }
                            w4 = LENGTH4_SIZE;
                        }
                    }
                    w3 = LENGTH3_SIZE;
                    w4 = LENGTH4_SIZE;
                }
            }
            w2 = LENGTH2_SIZE;
            w3 = LENGTH3_SIZE;
            w4 = LENGTH4_SIZE;
        }
        return -1;
    }

    @Override
    public int previousClearBit(int fromIndex) {
        if (fromIndex < 0) {
            if (fromIndex == -1) {
                return -1;
            }
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        }

        final long[][][] table = this.table;
        final int tableSize = table.length - 1;

        int w = wordIndex(fromIndex);
        int w1 = level1Index(w);
        if (w1 > tableSize) {
            return fromIndex;
        }
        int w2 = level2Index(w);
        int w3 = level3Index(w);
        int w4 = fromIndex % LENGTH4;

        long word;
        long[][] a2;
        long[] a3;

        for (; w1 >= 0; --w1) {
            if ((a2 = table[w1]) == null) {
                return bitIndex(w1, w2, w3) + w4;
            }
            for (; w2 >= 0; --w2) {
                if ((a3 = a2[w2]) == null) {
                    return bitIndex(w1, w2, w3) + w4;
                }
                for (; w3 >= 0; --w3) {
                    if ((word = a3[w3]) == 0) {
                        return bitIndex(w1, w2, w3) + w4;
                    }
                    for (int offset = w4; offset >= 0; --offset) {
                        if ((word & (1L << offset)) == 0) {
                            return bitIndex(w1, w2, w3) + offset;
                        }
                    }
                    w4 = LENGTH4_SIZE;
                }
                w3 = LENGTH3_SIZE;
            }
            w2 = LENGTH2_SIZE;
        }
        return -1;
    }

    @Override
    public boolean intersects(BitSet set) {
        if (this == set) {
            return true;
        }
        if (!(set instanceof SparseBitSet other)) {
            return super.intersects(set);
        }
        return iterateBlocks(this, other, new IntersectsAction(this));
    }

    private static class IntersectsAction extends BlockAction<Boolean> {

        private boolean intersects = false;

        private IntersectsAction(SparseBitSet self) {
            super(self);
        }

        @Override
        boolean accept(int w1, int w2, long[] selfBlock, long[] iteratedBlock) {
            boolean isZero = true;
            if (selfBlock != null) {
                for (int w3 = 0; w3 < LENGTH3; ++w3) {
                    long selfWord = selfBlock[w3];
                    if (selfWord != 0) {
                        long iteratedWord = iteratedBlock[w3];
                        if ((selfWord & iteratedWord) != 0) {
                            intersects = true;
                            return false;
                        }
                        isZero = false;
                    }
                }
            }
            return isZero;
        }

        @Override
        boolean canBreak() {
            // already found intersection, can break the iteration.
            return intersects;
        }

        @Override
        Boolean getResult() {
            return intersects;
        }
    }

    @Override
    public boolean contains(BitSet set) {
        if (this == set) {
            return true;
        }
        if (!(set instanceof SparseBitSet other)) {
            return super.contains(set);
        }
        return iterateBlocks(this, other, new ContainsAction(this));
    }

    private static class ContainsAction extends BlockAction<Boolean> {

        private boolean contains = true;

        private ContainsAction(SparseBitSet self) {
            super(self);
        }

        @Override
        boolean accept(int w1, int w2, long[] selfBlock, long[] iteratedBlock) {
            boolean isZero = true;
            if (selfBlock != null) {
                for (int w3 = 0; w3 < LENGTH3; ++w3) {
                    long selfWord = selfBlock[w3];
                    if (selfWord != 0) {
                        isZero = false;
                    }
                    long iteratedWord = iteratedBlock[w3];
                    if ((selfWord | iteratedWord) != selfWord) {
                        contains = false;
                        break;
                    }
                }
            } else {
                contains = false;
            }
            return isZero;
        }

        @Override
        boolean canBreak() {
            // already found not-contain bits, can break the itereation
            return !contains;
        }

        @Override
        Boolean getResult() {
            return contains;
        }
    }

    @Override
    public void clear() {
        clearTable(0);
    }

    @Override
    public boolean isEmpty() {
        updateState();
        return state.cardinality == 0;
    }

    @Override
    public int length() {
        updateState();
        return state.length;
    }

    @Override
    public int size() {
        updateState();
        return state.size;
    }

    @Override
    public int cardinality() {
        updateState();
        return state.cardinality;
    }

    // ------------------------------------------------------------------------
    // utility methods and classes
    // ------------------------------------------------------------------------

    private static int level1Index(int wordIndex) {
        return wordIndex >> SHIFT1;
    }

    private static int level2Index(int wordIndex) {
        return (wordIndex >> SHIFT2) & MASK2;
    }

    private static int level3Index(int wordIndex) {
        return wordIndex & MASK3;
    }

    private static int wordIndex(int w1, int w2, int w3) {
        return (w1 << SHIFT1) + (w2 << SHIFT2) + w3;
    }

    private static int bitIndex(int w1, int w2, int w3) {
        return wordIndex(w1, w2, w3) << SHIFT3;
    }

    private static @Nullable long[] getBlock(long[][][] table, int w1, int w2) {
        return table[w1] != null ? table[w1][w2] : null;
    }

    /**
     *  Resize the bit array. Moves the entries in the bits array of this
     *  SparseBitSet into an array whose size (which may be larger or smaller)
     *  is the given bit size (<i>i.e.</i>, includes the bit whose bitIndex is
     *  one less that the given value). If the new array is smaller, the excess
     *  entries in the set array are discarded. If the new array is bigger,
     *  it is filled with nulls.
     *
     * @param bitIndex the desired bit index to be included in the set
     */
    private void resize(int bitIndex) {
        //  Find an array size that is a power of two that is as least
        //  large enough to contain the bitIndex requested.
        final int w1 = level1Index(wordIndex(bitIndex));
        int newSize = Integer.highestOneBit(w1);
        if (newSize == 0) {
            newSize = 1;
        }
        if (w1 >= newSize) {
            newSize <<= 1;
        }
        if (newSize > MAX_LENGTH1) {
            newSize = MAX_LENGTH1;
        }
        final int aLength1 = (table != null ? table.length : 0);

        if (newSize != aLength1 || table == null) {
            // only if the size needs to be changed
            final long[][][] temp = new long[newSize][][]; //  Get the new array
            if (aLength1 != 0) {
                //  If it exists, copy old array to the new array.
                System.arraycopy(table, 0, temp, 0, Math.min(aLength1, newSize));
                clearTable(0); //  Don't leave unused pointers around.
            }
            table = temp; //  Set new array as the set array
            bitsLength = //  Index of last possible bit, plus one.
                    (newSize == MAX_LENGTH1 ? Integer.MAX_VALUE : newSize * UNIT);
        }
    }

    /**
     *  Clears out a part of the set array with nulls, from the given
     *  fromAreaIndex to the end of the array. If the given parameter
     *  is beyond the end of the bits array, nothing is changed.
     *
     * @param fromAreaIndex word index at which to start (inclusive)
     */
    private void clearTable(int fromAreaIndex) {
        final int aLength = table.length;
        if (fromAreaIndex < aLength) {
            for (int w = fromAreaIndex; w != aLength; ++w) {
                table[w] = null;
            }
            invalidateState();
        }
    }

    private static <R> R iterateBlocks(SparseBitSet self, SparseBitSet iterated,
                                       BlockAction<R> action) {
        assert self == action.self;
        long[][][] selfTable = self.table;
        long[][][] iteratedTable = iterated.table;
        if (iterated != self) {
            iterated.updateState(); // normalize the other set
        }
        action.start(iterated);
        outer:
        for (int w1 = 0; w1 < iteratedTable.length; ++w1) {
            long[][] iteratedArea = iteratedTable[w1];
            if (iteratedArea != null) {
                boolean isZeroArea = true;
                for (int w2 = 0; w2 < LENGTH2; ++w2) {
                    long[] iteratedBlock = iteratedArea[w2];
                    if (iteratedBlock != null) {
                        long[] selfBlock = getBlock(selfTable, w1, w2);
                        boolean isZeroBlock = action.accept(w1, w2,
                                selfBlock, iteratedBlock);
                        if (isZeroBlock) {
                            if (selfBlock != null) {
                                // clear zero block in self
                                selfTable[w1][w2] = null;
                            }
                        } else { // found non-zero block, then the area is not zero
                            isZeroArea = false;
                        }
                        if (action.canBreak()) {
                            break outer;
                        }
                    }
                }
                if (isZeroArea && selfTable[w1] != null) {
                    // clear zero area in self
                    selfTable[w1] = null;
                }
            }
        }
        action.finish();
        return action.getResult();
    }

    private static abstract class BlockAction<R> {

        final SparseBitSet self;

        BlockAction(SparseBitSet self) {
            this.self = self;
        }

        void start(SparseBitSet iterated) {
        }

        /**
         * @return {@code true} if {@code selfBlock} becomes zero block after
         * this call.
         */
        abstract boolean accept(int w1, int w2, long[] selfBlock, long[] iteratedBlock);

        boolean canAcceptNullBlock() {
            return false;
        }

        boolean acceptNullBlock(int w1, int w2, long[] selfBlock) {
            throw new UnsupportedOperationException();
        }

        boolean canBreak() {
            return false;
        }

        void finish() {
        }

        R getResult() {
            return null;
        }
    }

    private void invalidateState() {
        state.valid = false;
    }

    /**
     * The entirety of the bit set is examined, and the various statistics of
     * the bit set (size, length, cardinality, hashCode, etc.) are computed. Level
     * arrays that are empty (i.e., all zero at level 3, all null at level 2) are
     * replaced by null references, ensuring a normalized representation.
     */
    private void updateState() {
        if (!state.valid) {
            iterateBlocks(this, this, new UpdateAction(this));
        }
    }

    private static class UpdateAction extends BlockAction<Void> {

        /**
         *  Working space for find the size and length of the bit set. Holds the
         *  index of the first non-empty word in the set.
         */
        private transient int wMin;

        /**
         *  Working space for find the size and length of the bit set. Holds copy of
         *  the first non-empty word in the set.
         */
        private transient long wordMin;

        /**
         *  Working space for find the size and length of the bit set. Holds the
         *  index of the last non-empty word in the set.
         */
        private transient int wMax;

        /**
         *  Working space for find the size and length of the bit set. Holds a copy
         *  of the last non-empty word in the set.
         */
        private transient long wordMax;

        /**
         *  Working space for find the hash value of the bit set. Holds the
         *  current state of the computation of the hash value. This value is
         *  ultimately transferred to the Cache object.
         *
         * @see State
         */
        private transient long hash;

        /**
         *  Working space for keeping count of the number of non-zero words in the
         *  bit set. Holds the current state of the computation of the count. This
         *  value is ultimately transferred to the Cache object.
         *
         * @see State
         */
        private transient int count;

        /**
         * Number of blocks actually in use by this set to represent bit values.
         */
        private transient int blockCount;

        /**
         *  Working space for counting the number of non-zero bits in the bit set.
         *  Holds the current state of the computation of the cardinality.This
         *  value is ultimately transferred to the Cache object.
         *
         * @see State
         */
        private transient int cardinality;

        private UpdateAction(SparseBitSet self) {
            super(self);
        }

        @Override
        void start(SparseBitSet iterated) {
            hash = 1234L; // Magic number
            wMin = -1; // index of first non-zero word
            wordMin = 0L; // word at that index
            wMax = 0; // index of last non-zero word
            wordMax = 0L; // word at that index
            count = 0; // count of non-zero words in whole set
            blockCount = 0; // count of blocks actually use in whole set
            cardinality = 0; // count of non-zero bits in the whole set
        }

        @Override
        boolean accept(int w1, int w2, long[] selfBlock, long[] iteratedBlock) {
            ++blockCount;
            boolean isZero = true; //  Presumption
            for (int w3 = 0; w3 != LENGTH3; ++w3) {
                final long word = iteratedBlock[w3];
                if (word != 0) {
                    isZero = false;
                    compute(wordIndex(w1, w2, w3), word);
                }
            }
            return isZero;
        }

        /**
         *  This method does the accumulation of the statistics. It must be called
         *  in sequential order of the words in the set for which the statistics
         *  are being accumulated, and only for non-null values of the second
         *  parameter.
         *
         *  Two of the values (a2Count and a3Count) are not updated here,
         *  but are done in the code near where this method is called.
         *
         * @param index the word index of the word supplied
         * @param word  the long non-zero word from the set
         */
        private void compute(final int index, final long word) {
            // Count the number of actual words being used.
            ++count;
            // Continue to accumulate the hash value of the set.
            hash ^= word * (long) (index + 1);
            // The first non-zero word contains the first actual bit of the
            // set. The location of this bit is used to compute the set size.
            if (wMin < 0) {
                wMin = index;
                wordMin = word;
            }
            // The last non-zero word contains the last actual bit of the set.
            // The location of this bit is used to compute the set length.
            wMax = index;
            wordMax = word;
            // Count the actual bits, so as to get the cardinality of the set.
            cardinality += Long.bitCount(word);
        }

        @Override
        void finish() {
            State state = self.state;
            state.count = count;
            state.cardinality = cardinality;
            state.length = (wMax + 1) * LENGTH4 - Long.numberOfLeadingZeros(wordMax);
            state.size = blockCount * LENGTH3 * BITS_PER_WORD;
            state.hash = (int) ((hash >> Integer.SIZE) ^ hash);
            state.valid = true;
        }
    }

    private static class State {

        /**
         * Boolean value indicating whether the values in current state is valid;
         * if not, then {@link #updateState()} should be called to update.
         */
        private boolean valid;

        private transient int hash;

        private transient int size;

        private transient int cardinality;

        private transient int length;

        private transient int count;
    }

    // ------------------------------------------------------------------------
    // not implemented methods
    // ------------------------------------------------------------------------

    @Override
    public boolean and(BitSet set) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean andNot(BitSet set) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean xor(BitSet set) {
        throw new UnsupportedOperationException();
    }
}
