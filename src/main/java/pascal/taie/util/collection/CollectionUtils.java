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
import java.util.stream.Stream;

/**
 * Provides convenient utility operations for collections.
 */
public class CollectionUtils {

    private CollectionUtils() {
    }

    /**
     * @return an arbitrary element of the given collection.
     */
    public static <T> T getOne(Collection<? extends T> collection) {
        return collection.iterator().next();
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Adds a stream of elements to specific collection.
     */
    public static <T> void addAll(Collection<? super T> coll, Stream<? extends T> s) {
        s.forEach(coll::add);
    }
}
