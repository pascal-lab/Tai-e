/*
 * Tai-e: A Program Analysis Framework for Java
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

package pascal.taie.oldpta.set;

import pascal.taie.oldpta.core.cs.CSObj;

import java.util.Collection;
import java.util.stream.Stream;

public interface PointsToSet extends Iterable<CSObj> {

    boolean addObject(CSObj obj);

    boolean addAll(PointsToSet pts);

    Collection<CSObj> getObjects();

    boolean isEmpty();

    int size();

    Stream<CSObj> stream();
}
