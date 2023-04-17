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

/**
 * Provides common functionality for {@link IBitSet} implementations.
 * <p>
 * Especially, based on {@link IBitSet.Action}, it implements some operations
 * on {@link IBitSet} without the need to knowing its concrete type, so that
 * it support operations between bit sets of different types.
 */
public abstract class AbstractBitSet implements IBitSet {

    /*
     * BitSets are packed into arrays of "words."  Currently, a word is
     * a long, which consists of 64 bits, requiring 6 address bits.
     * The choice of word size is determined purely by performance concerns.
     */
    protected static final int ADDRESS_BITS_PER_WORD = 6;

    protected static final int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;

    /**
     * Given a bit index, return word index containing it.
     */
    protected static int wordIndex(int bitIndex) {
        return bitIndex >> ADDRESS_BITS_PER_WORD;
    }

    @Override
    public boolean set(int bitIndex, boolean value) {
        return value ? set(bitIndex) : clear(bitIndex);
    }

    @Override
    public boolean intersects(IBitSet set) {
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
    public boolean disjoints(IBitSet set) {
        return !intersects(set);
    }

    @Override
    public boolean contains(IBitSet set) {
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
    public boolean andNot(IBitSet set) {
        return set.iterateBits(new AndNotAction());
    }

    private class AndNotAction extends ChangeAction {

        @Override
        public boolean accept(int bitIndex) {
            if (clear(bitIndex)) {
                changed = true;
            }
            return true;
        }
    }

    @Override
    public boolean or(IBitSet set) {
        return set.iterateBits(new OrAction());
    }

    private class OrAction extends ChangeAction {

        @Override
        public boolean accept(int bitIndex) {
            if (set(bitIndex)) {
                changed = true;
            }
            return true;
        }
    }

    @Override
    public IBitSet orDiff(IBitSet set) {
        return set.iterateBits(new OrDiffAction());
    }

    private class OrDiffAction implements Action<IBitSet> {

        private final IBitSet diff = IBitSet.of();

        @Override
        public boolean accept(int bitIndex) {
            if (set(bitIndex)) {
                diff.set(bitIndex);
            }
            return true;
        }

        @Override
        public IBitSet getResult() {
            return diff;
        }
    }

    @Override
    public boolean xor(IBitSet set) {
        return set.iterateBits(new XorAction());
    }

    private class XorAction extends ChangeAction {

        @Override
        public boolean accept(int bitIndex) {
            flip(bitIndex);
            changed = true;
            return true;
        }
    }

    /**
     * Abstract class for the actions that may change this set.
     */
    private abstract static class ChangeAction implements Action<Boolean> {

        /**
         * Boolean value indicating whether this set changed by the action.
         */
        boolean changed = false;

        @Override
        public Boolean getResult() {
            return changed;
        }
    }

    @Override
    public void setTo(IBitSet set) {
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
                if (++i < 0) {
                    break;
                }
                if ((i = nextSetBit(i)) < 0) {
                    break;
                }
                int endOfRun = nextClearBit(i);
                do {
                    b.append(", ").append(i);
                } while (++i != endOfRun);
            }
        }

        b.append('}');
        return b.toString();
    }
}
