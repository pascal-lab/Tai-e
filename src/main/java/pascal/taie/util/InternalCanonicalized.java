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

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Marker annotation.
 * If a class is annotated by this annotation, it means that the class
 * applies internal canonicalization and the instances of the class
 * are canonicalized by the class internally.
 *
 * The annotated classes use the following pattern to canonicalize
 * their instances:
 * <ol>
 *     <li>use default equals() and hashCode() (allow fast comparison)
 *     <li>create a private class named Key, which overrides equals() and hashCode()
 *     <li>maintain a hash map from Key to instance
 *     <li>hide constructor, and provide static factory method for
 *     obtaining instances, and use the map for canonicalization
 * </ol>
 */
@Target(ElementType.TYPE)
public @interface InternalCanonicalized {
}
