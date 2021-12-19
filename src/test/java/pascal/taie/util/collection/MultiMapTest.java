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

package pascal.taie.util.collection;

import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

public class MultiMapTest {

    @Test
    public void testPut() {
        MultiMap<Integer, Integer> m = Maps.newMultiMap();
        m.put(1, 1);
        m.put(1, 2);
        m.put(1, 3);
        Assert.assertTrue(m.put(2, 1));
        Assert.assertFalse(m.put(2, 1));
        Assert.assertFalse(m.put(2, 1));
        Assert.assertEquals(4, m.size());
    }

    @Test
    public void testPutAll1() {
        MultiMap<Integer, Integer> m = Maps.newMultiMap();
        Assert.assertTrue(m.putAll(1, Set.of(6, 7, 8)));
        Assert.assertTrue(m.putAll(2, Set.of(1)));
        Assert.assertFalse(m.putAll(3, Set.of()));
        Assert.assertEquals(4, m.size());
    }

    @Test
    public void testPutAll2() {
        MultiMap<Integer, Integer> m1 = Maps.newMultiMap();
        m1.putAll(1, Set.of(6, 7, 8));
        m1.putAll(2, Set.of(1));

        MultiMap<Integer, Integer> m2 = Maps.newMultiMap();
        Assert.assertTrue(m2.putAll(m1));
        System.out.println(m2);
        Assert.assertEquals(4, m2.size());
    }

    @Test
    public void testRemove() {
        MultiMap<Integer, Integer> m = Maps.newMultiMap();
        m.putAll(1, Set.of(555, 888, 666));
        m.putAll(2, Set.of(777));
        m.remove(1, 666);
        m.remove(2, 777);
        Assert.assertEquals(2, m.size());
    }

    @Test
    public void testRemoveAll() {
        MultiMap<Integer, Integer> m = Maps.newMultiMap();
        m.putAll(1, Set.of(314, 159, 265));
        m.putAll(2, Set.of(3));
        Assert.assertTrue(m.removeAll(1));
        Assert.assertTrue(m.removeAll(2, Set.of(3, 5, 7)));
        Assert.assertTrue(m.isEmpty());
        Assert.assertFalse(m.containsKey(2));

        m.putAll(1, Set.of(314, 159, 265));
        m.removeAll(1, Set.of(314, 159, 265));
        Assert.assertFalse(m.containsKey(1));
    }

    @Test
    public void testGet() {
        MultiMap<Integer, Integer> m = Maps.newMultiMap();
        m.putAll(1, Set.of(314, 159, 265));
        m.putAll(2, Set.of(3));
        Assert.assertEquals(3, m.get(1).size());
        Assert.assertEquals(1, m.get(2).size());
        Assert.assertEquals(0, m.get(3).size());
    }

    @Test
    public void testContains() {
        MultiMap<Integer, Integer> m = Maps.newMultiMap();
        m.putAll(1, Set.of(314, 159, 265));
        m.putAll(2, Set.of(3));

        Assert.assertTrue(m.contains(1, 314));
        Assert.assertTrue(m.containsKey(2));
        Assert.assertTrue(m.containsValue(3));

        Assert.assertFalse(m.contains(1, 2333));
        Assert.assertFalse(m.containsKey(2333));
        Assert.assertFalse(m.containsValue(2333));

        m.remove(2, 3);
        Assert.assertFalse(m.containsKey(2));
    }

    @Test
    public void testEquals() {
        MultiMap<Integer, Integer> m1 = Maps.newMultiMap();
        m1.putAll(1, Set.of(314, 159, 265));
        m1.putAll(2, Set.of(3));

        MultiMap<Integer, Integer> m2 = Maps.newMultiMap();
        m2.putAll(m1);
        Assert.assertEquals(m1, m2);
        m2.remove(1, 314);
        Assert.assertNotEquals(m1, m2);
        m2.put(1, 314);
        Assert.assertEquals(m1, m2);
        m2.put(1, 3);
        Assert.assertNotEquals(m1, m2);

        m1.clear();
        Assert.assertNotEquals(m1, m2);
        m2.clear();
        Assert.assertEquals(m1, m2);
    }
}
