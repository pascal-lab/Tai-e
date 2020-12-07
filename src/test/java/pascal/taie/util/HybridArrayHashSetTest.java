/*
 * Tai'e - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai'e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.util;

import org.junit.Test;

import java.util.Set;

public class HybridArrayHashSetTest extends AbstractSetTest {

    @Override
    protected <E> Set<E> newSet() {
        return new HybridArrayHashSet<>();
    }

    @Test
    public void testAdd20() {
        testAddNElements(newSet(), 20);
    }
}
