/*
 * Panda - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Panda is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.panda.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class ArraySetTest {

    @Test
    public void testAdd() {
        ArraySet<String> set = new ArraySet<>();
        set.add("a");
        set.add("a");
        set.add("b");
        set.add("c");
        Assert.assertEquals(3, set.size());
    }

    @Test(expected = NullPointerException.class)
    public void testAddNull() {
        ArraySet<String> set = new ArraySet<>();
        set.add("a");
        set.add(null);
        set.add("b");
    }

    @Test(expected = TooManyElementsException.class)
    public void testFixedCapacity() {
        ArraySet<String> set = new ArraySet<>(4);
        set.add("a");
        set.add("b");
        set.add("c");
        set.add("d");
        set.add("e");
    }

    @Test
    public void testAddAll() {
        ArraySet<String> set = new ArraySet<>();
        set.addAll(Arrays.asList("a", "a", "b", "c", "c"));
        Assert.assertEquals(3, set.size());
    }

    @Test
    public void testRemove() {
        ArraySet<String> set = new ArraySet<>();
        set.add("a");
        set.add("b");
        set.add("c");
        Assert.assertEquals(3, set.size());
        set.remove("x");
        Assert.assertEquals(3, set.size());
        set.remove("a");
        Assert.assertEquals(2, set.size());
        set.remove("b");
        Assert.assertEquals(1, set.size());
    }

    @Test
    public void testEmpty() {
        ArraySet<String> set = new ArraySet<>();
        Assert.assertEquals(0, set.size());
        set.remove("x");
        Assert.assertEquals(0, set.size());
    }

    @Test
    public void testNonFixedCapacity() {
        ArraySet<String> set = new ArraySet<>(4, false);
        set.add("a");
        set.add("b");
        set.add("c");
        set.add("d");
        set.add("e");
        Assert.assertEquals(5, set.size());
    }
}
