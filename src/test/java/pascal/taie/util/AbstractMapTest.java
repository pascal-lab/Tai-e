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

import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("ALL")
abstract class AbstractMapTest {

    protected abstract <K, V> Map<K, V> newMap();

    @Test
    public void testPut() {
        Map<Integer, String> map = newMap();
        map.put(1, "a");
        map.put(1, "b");
        map.put(2, "c");
        map.put(3, "d");
        Assert.assertEquals(3, map.size());
        Assert.assertEquals("b", map.get(1));
    }

    @Test(expected = NullPointerException.class)
    public void testPutNullKey() {
        Map<String, Object> map = newMap();
        map.put("x", new Object());
        map.put(null, new Object());
    }

    @Test
    public void testKeySet() {
        Map<Integer, String> map = newMap();
        Set<Integer> keySet = map.keySet();
        Assert.assertEquals(0, keySet.size());
        map.put(1, "x");
        map.put(2, "y");
        Assert.assertEquals(2, keySet.size());
        keySet.remove(1);
        Assert.assertEquals(1, map.size());
    }

    @Test
    public void testKeySet20() {
        testKeySetN(newMap(), 20);
    }

    void testKeySetN(Map<Integer, String> map, int n) {
        map.clear();
        Set<Integer> keySet = map.keySet();
        for (int i = 0; i < n; ++i) {
            map.put(i, "");
        }
        Assert.assertEquals(n, keySet.size());
    }

    @Test
    public void testKeySetIterator() {
        Map<Integer, String> map = newMap();
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
