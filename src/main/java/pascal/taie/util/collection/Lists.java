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
     * Finds the first element of given list that satisfies the given predicate.
     * If not such element is found, returns null.
     */
    @Nullable
    public static <T> T findFirst(List<T> list, Predicate<T> p) {
        for (T e : list) {
            if (p.test(e)) {
                return e;
            }
        }
        return null;
    }

    /**
     * Applies a mapper function on a given collection and returns
     * the resulting list. The resulting list is immutable.
     */
    public static <T, R> List<R> map(Collection<T> c, Function<T, R> mapper) {
        return c.isEmpty() ? List.of() : c.stream().map(mapper).toList();
    }
}
