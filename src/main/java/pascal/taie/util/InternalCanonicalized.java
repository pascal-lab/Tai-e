/*
 * Tai-e - A Program Analysis Framework for Java
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

/**
 * Marker annotation.
 * If a class is annotated by this annotation, it means that the class
 * applies internal canonicalization and the instances of the class
 * are canonicalized by the class internally.
 */
/*
 * The annotated classes use the following pattern to canonicalize
 * their instances:
 * 1. use default equals() and hashCode() (allow fast comparison)
 * 2. create a private class named Key, which overrides equals() and hashCode()
 * 3. maintain a hash map from Key to instance
 * 4. hide constructor, and provide static factory method for
 *    obtaining instances, and use the map for canonicalization
 */
public @interface InternalCanonicalized {
}
