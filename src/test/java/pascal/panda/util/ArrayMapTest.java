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

import java.util.Iterator;
import java.util.Set;

@SuppressWarnings("ALL")
public class ArrayMapTest {

    @Test
    public void testPut() {
        ArrayMap<Integer, String> map = new ArrayMap<>();
        map.put(1, "a");
        map.put(1, "b");
        map.put(2, "c");
        map.put(3, "d");
        Assert.assertEquals(3, map.size());
        Assert.assertEquals("b", map.get(1));
    }

    @Test(expected = NullPointerException.class)
    public void testPutNullKey() {
        ArrayMap<String, Object> map = new ArrayMap<>();
        map.put("x", new Object());
        map.put(null, new Object());
    }

    @Test
    public void testKeySet() {
        ArrayMap<Integer, String> map = new ArrayMap<>();
        Set<Integer> keySet = map.keySet();
        Assert.assertEquals(0, keySet.size());
        map.put(1, "x");
        map.put(2, "y");
        Assert.assertEquals(2, keySet.size());
        keySet.remove(1);
        Assert.assertEquals(1, map.size());
    }

    @Test
    public void testKeySetIterator() {
        ArrayMap<Integer, String> map = new ArrayMap<>();
        map.put(1, "x");
        map.put(2, "y");
        map.put(3, "z");
        Assert.assertEquals(3, map.size());
        Set<Integer> keySet = map.keySet();
        Iterator<Integer> ksIt = keySet.iterator();
        while (ksIt.hasNext()) {
            int n = ksIt.next();
            ksIt.remove();
        }
        Assert.assertTrue(map.isEmpty());
    }
}
