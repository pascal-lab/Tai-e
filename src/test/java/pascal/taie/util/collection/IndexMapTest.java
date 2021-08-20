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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class IndexMapTest {

    private static final Indexer indexer = o -> ((Integer) o);

    private static Map<Integer, String> makeMap() {
        Map<Integer, String> m = new IndexMap<>(indexer, 6);
        m.put(1, "one");
        m.put(3, "three");
        m.put(4, "four");
        return m;
    }

    @Test
    public void testContainsKey() {
        var m = makeMap();
        Assert.assertTrue(m.containsKey(1));
        Assert.assertFalse(m.containsKey(100));
    }

    @Test
    public void testGet() {
        var m = makeMap();
        Assert.assertEquals("one", m.get(1));
        Assert.assertNull(m.get(100));
    }

    @Test
    public void testPut() {
        var m = makeMap();
        Assert.assertEquals(m.get(1), "one");
        m.put(1, "ONE");
        Assert.assertEquals(m.get(1), "ONE");
    }

    @Test
    public void testRemove() {
        var m = makeMap();
        Assert.assertEquals(3, m.size());
        m.remove(2);
        Assert.assertEquals(3, m.size());
        m.remove(1);
        Assert.assertEquals(2, m.size());
        m.remove(3);
        Assert.assertEquals(1, m.size());
        m.remove(4);
        Assert.assertEquals(0, m.size());
        m.remove(4);
        Assert.assertEquals(0, m.size());
    }

    @Test
    public void testClear() {
        var m = makeMap();
        Assert.assertEquals(3, m.size());
        m.clear();
        Assert.assertEquals(0, m.size());
    }

    @Test
    public void testForEach() {
        var s = new HashSet<>(Set.of("one", "three", "four"));
        var m = makeMap();
        m.forEach((k, v) -> s.remove(v));
        Assert.assertTrue(s.isEmpty());
    }

    @Test
    public void testIterator() {
        var m = makeMap();
        var iter = m.entrySet().iterator();
        while (iter.hasNext()) {
            iter.next();
            iter.remove();
        }
        Assert.assertTrue(m.isEmpty());
    }

    @Test(expected = IllegalIndexException.class)
    public void testInvalidIndex() {
        var m = makeMap();
        m.put(20, "");
    }
}
