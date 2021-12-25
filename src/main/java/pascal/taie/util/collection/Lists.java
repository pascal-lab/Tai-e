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

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Utility methods for {@link List}.
 */
public final class Lists {

    private Lists() {
    }

    /**
     * Applies a mapper function on a given collection and returns
     * the results as a list. The resulting list is unmodifiable.
     */
    public static <T, R> List<R> map(Collection<? extends T> c,
                                     Function<T, R> mapper) {
        return c.isEmpty() ? List.of() : c.stream().map(mapper).toList();
    }

    /**
     * Tests the elements in a given collection and returns a list of elements
     * that can pass the test. The resulting list is unmodifiable.
     */
    public static <T> List<T> filter(Collection<T> c,
                                     Predicate<? super T> predicate) {
        List<T> result = c.stream().filter(predicate).toList();
        return result.isEmpty() ? List.of() : result;
    }
}
