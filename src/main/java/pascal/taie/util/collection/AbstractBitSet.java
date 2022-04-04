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

    @Override
    public boolean set(int bitIndex, boolean value) {
        return value ? set(bitIndex) : clear(bitIndex);
    }

    @Override
    public boolean intersects(BitSet set) {
        return set.iterateBits(new IntersectsAction());
    }

    private class IntersectsAction implements Action<Boolean> {

        private boolean intersects = false;

        @Override
        public boolean accept(int bitIndex) {
            if (get(bitIndex)) {
                intersects = true;
                return false;
            }
            return true;
        }

        @Override
        public Boolean getResult() {
            return intersects;
        }
    }

    @Override
    public boolean disjoints(BitSet set) {
        return !intersects(set);
    }

    @Override
    public boolean contains(BitSet set) {
        return set.iterateBits(new ContainsAction());
    }

    private class ContainsAction implements Action<Boolean> {

        private boolean contains = true;

        @Override
        public boolean accept(int bitIndex) {
            if (!get(bitIndex)) {
                contains = false;
                return false;
            }
            return true;
        }

        @Override
        public Boolean getResult() {
            return contains;
        }
    }

    @Override
    public boolean or(BitSet set) {
        return set.iterateBits(new OrAction());
    }

    private class OrAction implements Action<Boolean> {

        private boolean changed = false;

        @Override
        public boolean accept(int bitIndex) {
            if (set(bitIndex)) {
                changed = true;
            }
            return true;
        }

        @Override
        public Boolean getResult() {
            return changed;
        }
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
}
