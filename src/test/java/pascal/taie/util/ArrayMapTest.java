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

package pascal.taie.util;

import org.junit.Test;

import java.util.Map;

public class ArrayMapTest extends AbstractMapTest {

    @Override
    protected <K, V> Map<K, V> newMap() {
        return new ArrayMap<>();
    }

    @Test(expected = TooManyElementsException.class)
    public void testKeySet20() {
        super.testKeySet20();
    }
}
