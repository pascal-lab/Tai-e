/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.util;

import java.util.Collection;
import java.util.function.Function;


public interface CollectionView<From, To> extends Collection<To> {

    static <From, To> CollectionView<From, To> of(
            Collection<From> collection, Function<From, To> mapper) {
        return new ImmutableCollectionView<>(collection, mapper);
    }
}
