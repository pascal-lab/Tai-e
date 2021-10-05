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
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
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
     * @return an arbitrary element of the given collection.
     * @throws java.util.NoSuchElementException if the stream is empty
     */
    public static <T> T getOne(Stream<? extends T> stream) {
        Optional<? extends T> one = stream.findAny();
        if (one.isPresent()) {
            return one.get();
        } else {
            throw new NoSuchElementException("The stream is empty");
        }
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
}
