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

public class ListUtils {

    private ListUtils() {
    }

    /**
     * Cons an element to a collection and returns the resulting list.
     * This API is equivalent to the cons operation in Lisp.
     */
    public static <T> List<T> cons(T e, Collection<T> c) {
        List<T> result = new ArrayList<>(1 + c.size());
        result.add(e);
        result.addAll(c);
        return result;
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
