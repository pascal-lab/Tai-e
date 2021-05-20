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

package pascal.taie.util;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Strings {

    // Suppresses default constructor, ensuring non-instantiability.
    private Strings() {
    }

    /**
     * Converts a stream to a string.
     * The the elements in the collection are sorted by their
     * string representation (in alphabet order) in the resulting string.
     * This is particularly useful for comparing expected results with the ones
     * given by the analysis.
     */
    public static <T> String toString(Stream<T> stream) {
        return "[" + stream.map(T::toString)
                .sorted()
                .collect(Collectors.joining(", ")) + "]";
    }

    /**
     * Converts a collection to a string.
     * The the elements in the collection are sorted by their
     * string representation (in alphabet order) in the resulting string.
     * This is particularly useful for comparing expected results with the ones
     * given by the analysis.
     */
    public static <T> String toString(Collection<? extends T> coll) {
        return toString(coll.stream());
    }
}
