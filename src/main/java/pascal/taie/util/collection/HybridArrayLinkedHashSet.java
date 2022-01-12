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

import java.util.HashSet;
import java.util.LinkedHashSet;

public class HybridArrayLinkedHashSet<E> extends HybridArrayHashSet<E> {

    @Override
    protected HashSet<E> newSet(int initialCapacity) {
        return new LinkedHashSet<>(initialCapacity);
    }
}
