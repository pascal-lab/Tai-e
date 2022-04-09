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

import java.util.AbstractSet;
import java.util.Collection;

public abstract class AbstractEnhancedSet<E> extends AbstractSet<E>
        implements EnhancedSet<E> {

    @Override
    public EnhancedSet<E> copy() {
        EnhancedSet<E> copy = newSet();
        copy.addAll(this);
        return this;
    }

    @Override
    public EnhancedSet<E> addAllDiff(Collection<? extends E> c) {
        EnhancedSet<E> diff = newSet();
        for (E e : c) {
            if (add(e)) {
                diff.add(e);
            }
        }
        return diff;
    }

    /**
     * Creates and returns a new set. The type of the new set should be the
     * corresponding subclass.
     * This method is provided to ease the implementation of {@link #copy()}
     * and {@link #addAllDiff(Collection)}. If a subclass overwrites
     * above two methods, it does not need to re-implement this method.
     */
    protected EnhancedSet<E> newSet() {
        throw new UnsupportedOperationException();
    }
}
