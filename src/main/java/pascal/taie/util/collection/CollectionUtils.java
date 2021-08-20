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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utility methods for {@link Collection}.
 * We name it CollectionUtils instead of Collections to avoid name collision
 * with {@link java.util.Collections}.
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

    /**
     * Creates a list of given collection, appends a specific element to
     * the list and returns it.
     */
    public static <T> List<T> append(Collection<T> c, T e) {
        List<T> result = new ArrayList<>(c.size() +  1);
        result.addAll(c);
        result.add(e);
        return result;
    }
}
