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

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Static utility methods for {@link Stream}.
 */
public final class Streams {

    private Streams() {
    }

    /**
     * Leverages {@link Stream#concat(Stream, Stream)} to create a lazily
     * concatenated stream whose elements are all the elements of multiple
     * given streams. The resulting stream is ordered if all input streams
     * are ordered, and parallel if one of the input streams is parallel.
     *
     * @param streams the streams to be concatenated
     * @param <T>     the type of stream elements
     * @return the concatenated stream
     */
    @SafeVarargs
    public static <T> Stream<T> concat(Stream<? extends T>... streams) {
        Stream<T> result = Stream.of();
        for (int i = streams.length - 1; i >= 0; --i) {
            result = Stream.concat(streams[i], result);
        }
        return result;
    }

    /**
     * Converts a stream to a string.
     * The elements in the collection are sorted by their string representation
     * (in alphabet order) in the resulting string. This is particularly useful
     * for comparing expected results with the ones given by the analysis.
     */
    public static <T> String toString(Stream<T> stream) {
        return "[" + stream.map(T::toString)
                .sorted()
                .collect(Collectors.joining(", ")) + "]";
    }
}
