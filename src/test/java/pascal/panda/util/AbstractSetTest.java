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
import java.util.Set;

@SuppressWarnings("ALL")
abstract class AbstractSetTest {

    protected abstract <E> Set<E> newSet();

    @Test
    public void testAdd() {
        Set<String> set = newSet();
        set.add("a");
        set.add("a");
        set.add("b");
        set.add("c");
        Assert.assertEquals(3, set.size());
    }

    @Test(expected = NullPointerException.class)
    public void testAddNull() {
        Set<String> set = newSet();
        set.add("a");
        set.add(null);
        set.add("b");
    }

    void testAddNElements(Set<Integer> set, int n) {
        set.clear();
        for (int i = 0; i < n; ++i) {
            set.add(i);
        }
        Assert.assertEquals(n, set.size());
    }

    @Test
    public void testAddAll() {
        Set<String> set = newSet();
        set.addAll(Arrays.asList("a", "a", "b", "c", "c"));
        Assert.assertEquals(3, set.size());
    }

    @Test
    public void testRemove() {
        Set<String> set = newSet();
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
        Set<String> set = newSet();
        Assert.assertEquals(0, set.size());
        set.remove("x");
        Assert.assertEquals(0, set.size());
    }
}
