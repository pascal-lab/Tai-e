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
import java.util.Collections;
import java.util.List;

public class ListUtils {

    private ListUtils() {
    }

    /**
     * @return an unmodifiable list of the given elements.
     */
    public static <T> List<T> freeze(Collection<T> elements) {
        switch (elements.size()) {
            case 0:
                return Collections.emptyList();
            case 1:
                return Collections.singletonList(CollectionUtils.getOne(elements));
            default:
                return Collections.unmodifiableList(new ArrayList<>(elements));
        }
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

    /**
     * @return a list of concatenation of given collections.
     */
    public static <T> List<T> concat(Collection<T> c1, Collection<T> c2) {
        List<T> result = new ArrayList<>(c1.size() + c2.size());
        result.addAll(c1);
        result.addAll(c2);
        return result;
    }

    /**
     * Creates a list of concatenation of given collections,
     * appends a specific element to the list and returns it.
     */
    public static <T> List<T> concatAndAppend(Collection<T> c1, Collection<T> c2, T e) {
        List<T> result = new ArrayList<>(c1.size() + c2.size() + 1);
        result.addAll(c1);
        result.addAll(c2);
        result.add(e);
        return result;
    }
}
