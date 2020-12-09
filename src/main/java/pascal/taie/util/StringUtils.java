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

import java.util.stream.Stream;

public class StringUtils {

    // Suppresses default constructor, ensuring non-instantiability.
    private StringUtils() {
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
