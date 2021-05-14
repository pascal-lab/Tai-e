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

package pascal.taie.analysis.dfa;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

import static pascal.taie.util.collection.SetUtils.newHybridSet;

/**
 * TODO: implement copy-on-write?
 * @param <T> type of elements
 */
public class SetFlow<T> {

    private final Set<T> set;

    private SetFlow() {
        set = newHybridSet();
    }

    private SetFlow(Collection<T> c) {
        set = newHybridSet(c);
    }

    public static <T> SetFlow<T> make() {
        return new SetFlow<>();
    }

    public static <T> SetFlow<T> make(Collection<T> c) {
        return new SetFlow<>(c);
    }

    public Stream<T> stream() {
        return set.stream();
    }
}
