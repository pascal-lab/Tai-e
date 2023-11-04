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

import pascal.taie.util.Indexer;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

/**
 * Hybrid set that uses bit set for large set.
 * <p>
 * It is important to note that batch operations such as {@link #addAll(Collection)},
 * {@link #removeAll(Collection)}, {@link #retainAll(Collection)}, and {@link #containsAll(Collection)}
 * can experience performance degradation when used with {@link IndexerBitSet}.
 * <p>
 * For example, when removing elements of a {@link HybridBitSet} from an {@link IndexerBitSet}
 * (i.e., {@code indexerBitSet.removeAll(hybridBitSet)}) or vice versa,
 * the operation does not take advantage of bit-level operations.
 * Instead, it falls back on an iterative removal process that is less efficient.
 * <p>
 * Users are advised to be aware of these potential performance implications when performing
 * batch operations between {@link IndexerBitSet} and {@link HybridBitSet} instances
 * to avoid unexpected inefficiencies.
 *
 * @see IndexerBitSet
 */
public final class HybridBitSet<E> extends AbstractHybridSet<E>
        implements Serializable {

    private final Indexer<E> indexer;

    private final boolean isSparse;

    public HybridBitSet(Indexer<E> indexer, boolean isSparse) {
        this.indexer = indexer;
        this.isSparse = isSparse;
    }

    @Override
    protected Set<E> newLargeSet(int unused) {
        return new IndexerBitSet<>(indexer, isSparse);
    }

    @Override
    public boolean addAll(@Nonnull Collection<? extends E> c) {
        // optimize when c is HybridBitSet
        if (this.set instanceof GenericBitSet<E> thisBitSet
                && c instanceof HybridBitSet<? extends E> other
                && other.set instanceof GenericBitSet<? extends E> otherBitSet) {
            return thisBitSet.addAll(otherBitSet);
        }
        return super.addAll(c);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        // optimize when c is HybridBitSet
        if (this.set instanceof GenericBitSet<E> thisBitSet
                && c instanceof HybridBitSet<?> other
                && other.set instanceof GenericBitSet<?> otherBitSet) {
            return thisBitSet.containsAll(otherBitSet);
        }
        return super.containsAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        // optimize when c is HybridBitSet
        if (this.set instanceof GenericBitSet<E> thisBitSet
                && c instanceof HybridBitSet<?> other
                && other.set instanceof GenericBitSet<?> otherBitSet) {
            return thisBitSet.removeAll(otherBitSet);
        }
        return super.removeAll(c);
    }

    @Override
    public boolean retainAll(@Nonnull Collection<?> c) {
        // optimize when c is HybridBitSet
        if (this.set instanceof GenericBitSet<E> thisBitSet
                && c instanceof HybridBitSet<?> other
                && other.set instanceof GenericBitSet<?> otherBitSet) {
            return thisBitSet.retainAll(otherBitSet);
        }
        return super.retainAll(c);
    }

    @Override
    public HybridBitSet<E> addAllDiff(Collection<? extends E> c) {
        HybridBitSet<E> diff = new HybridBitSet<>(indexer, isSparse);
        if (c instanceof HybridBitSet other && other.isLargeSet) {
            //noinspection unchecked
            SetEx<E> otherSet = (SetEx<E>) other.set;
            Set<E> diffSet;
            if (set == null) {
                set = otherSet.copy();
                diffSet = otherSet.copy();
                if (singleton != null) {
                    set.add(singleton);
                    diffSet.remove(singleton);
                    singleton = null;
                }
            } else if (!isLargeSet) {
                // current set is small set
                Set<E> oldSet = set;
                set = otherSet.copy();
                diffSet = otherSet.copy();
                set.addAll(oldSet);
                diffSet.removeAll(oldSet);
            } else {
                // current set is already a large set
                diffSet = ((SetEx<E>) set).addAllDiff(otherSet);
            }
            diff.set = diffSet;
            diff.isLargeSet = isLargeSet = true;
        } else {
            for (E e : c) {
                if (add(e)) {
                    diff.add(e);
                }
            }
        }
        return diff;
    }

    @Override
    public HybridBitSet<E> copy() {
        HybridBitSet<E> copy = new HybridBitSet<>(indexer, isSparse);
        copy.singleton = singleton;
        copy.isLargeSet = isLargeSet;
        if (set != null) {
            copy.set = ((SetEx<E>) set).copy();
        }
        return copy;
    }
}
