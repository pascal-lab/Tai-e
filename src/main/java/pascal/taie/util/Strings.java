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

import java.util.stream.Stream;

public class Strings {

    // Suppresses default constructor, ensuring non-instantiability.
    private Strings() {
    }

    /**
     * Converts a stream to a string.
     */
    public static <T> String streamToString(Stream<T> stream) {
        Iterable<String> elems = () -> stream.map(T::toString)
                .sorted()
                .iterator();
        return "{" + String.join(",", elems) + "}";
    }
}
