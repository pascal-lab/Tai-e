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

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Static utility methods for {@link Stream}.
 */
public final class Streams {

    private Streams() {
    }

    /**
     * @return true if the given stream is empty, otherwise false.
     */
    public static <T> boolean isEmpty(Stream<T> stream) {
        return stream.findAny().isEmpty();
    }

    /**
     * @return a reversed version of given stream.
     */
    public static <T> Stream<T> reverse(Stream<T> stream) {
        Iterator<T> iterator = stream.collect(
                        Collectors.toCollection(ArrayDeque::new))
                .descendingIterator();
        Iterable<T> iterable = () -> iterator;
        return StreamSupport.stream(iterable.spliterator(), false);
    }
}
