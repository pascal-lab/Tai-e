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

public abstract class AbstractBitSet implements BitSet {

    /*
     * BitSets are packed into arrays of "words."  Currently a word is
     * a long, which consists of 64 bits, requiring 6 address bits.
     * The choice of word size is determined purely by performance concerns.
     */
    protected static final int ADDRESS_BITS_PER_WORD = 6;
    protected static final int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;

    /**
     * Checks that fromIndex ... toIndex is a valid range of bit indices.
     */
    protected static void checkRange(int fromIndex, int toIndex) {
        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        if (toIndex < 0)
            throw new IndexOutOfBoundsException("toIndex < 0: " + toIndex);
        if (fromIndex > toIndex)
            throw new IndexOutOfBoundsException("fromIndex: " + fromIndex +
                    " > toIndex: " + toIndex);
    }

    @Override
    public boolean set(int bitIndex, boolean value) {
        return value ? set(bitIndex) : clear(bitIndex);
    }

    @Override
    public boolean disjoints(BitSet set) {
        return !intersects(set);
    }

    @Override
    public boolean contains(BitSet set) {
        return set.iterateBits(new ContainsAction());
    }

    @Override
    public boolean or(BitSet set) {
        return set.iterateBits(new OrAction());
    }

    @Override
    public void setTo(BitSet set) {
        clear();
        or(set);
    }

    /**
     * Returns a string representation of this bit set. For every index
     * for which this {@code BitSet} contains a bit in the set
     * state, the decimal representation of that index is included in
     * the result. Such indices are listed in order from lowest to
     * highest, separated by ",&nbsp;" (a comma and a space) and
     * surrounded by braces, resulting in the usual mathematical
     * notation for a set of integers.
     *
     * <p>Example:
     * <pre>
     * BitSet drPepper = new BitSet();</pre>
     * Now {@code drPepper.toString()} returns "{@code {}}".
     * <pre>
     * drPepper.set(2);</pre>
     * Now {@code drPepper.toString()} returns "{@code {2}}".
     * <pre>
     * drPepper.set(4);
     * drPepper.set(10);</pre>
     * Now {@code drPepper.toString()} returns "{@code {2, 4, 10}}".
     *
     * @return a string representation of this bit set
     */
    public String toString() {
        final int MAX_INITIAL_CAPACITY = Integer.MAX_VALUE - 8;
        int numBits = cardinality();
        // Avoid overflow in the case of a humongous numBits
        int initialCapacity = (numBits <= (MAX_INITIAL_CAPACITY - 2) / 6) ?
                6 * numBits + 2 : MAX_INITIAL_CAPACITY;
        StringBuilder b = new StringBuilder(initialCapacity);
        b.append('{');

        int i = nextSetBit(0);
        if (i != -1) {
            b.append(i);
            while (true) {
                if (++i < 0) break;
                if ((i = nextSetBit(i)) < 0) break;
                int endOfRun = nextClearBit(i);
                do { b.append(", ").append(i); }
                while (++i != endOfRun);
            }
        }

        b.append('}');
        return b.toString();
    }

    private class ContainsAction implements Action<Boolean> {

        private boolean contains = true;

        @Override
        public void accept(int index) {
            if (!get(index)) {
                contains = false;
            }
        }

        @Override
        public boolean isBreak() {
            return !contains;
        }

        @Override
        public Boolean getResult() {
            return contains;
        }
    }

    private class OrAction implements Action<Boolean> {

        private boolean changed = false;

        @Override
        public void accept(int index) {
            if (set(index)) {
                changed = true;
            }
        }

        @Override
        public boolean isBreak() {
            return false;
        }

        @Override
        public Boolean getResult() {
            return changed;
        }
    }
}
