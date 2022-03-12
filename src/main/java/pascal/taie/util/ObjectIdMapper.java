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

/**
 * Maintains mappings between objects and integers (id). Each object in
 * the same mapper has a unique id, but different mappers may map different
 * objects (ids) to the same id (object), so it should be used with care.
 * <p>
 * The objects in a mapper {@code m} should preserve the invariant:
 * <code>e.equals(m.getObject(m.getId(e)))</code>.
 *
 * @param <E> type of elements
 */
public interface ObjectIdMapper<E> {

    /**
     *@return the id of the given object.
     */
    int getId(E o);

    /**
     * @return the corresponding object of the given id.
     */
    E getObject(int id);
}
