/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.util.collection;

import org.junit.Assert;
import org.junit.Test;
import pascal.taie.util.Indexer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class IndexMapTest {

    private static final Indexer<Integer> indexer = new Indexer<>() {

        @Override
        public int getIndex(Integer i) {
            return i;
        }

        @Override
        public Integer getObject(int index) {
            return index;
        }
    };

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
        assertEquals("one", m.get(1));
        Assert.assertNull(m.get(100));
    }

    @Test
    public void testPut() {
        var m = makeMap();
        assertEquals(m.get(1), "one");
        m.put(1, "ONE");
        assertEquals(m.get(1), "ONE");
    }

    @Test
    public void testRemove() {
        var m = makeMap();
        assertEquals(3, m.size());
        m.remove(2);
        assertEquals(3, m.size());
        m.remove(1);
        assertEquals(2, m.size());
        m.remove(3);
        assertEquals(1, m.size());
        m.remove(4);
        assertEquals(0, m.size());
        m.remove(4);
        assertEquals(0, m.size());
    }

    @Test
    public void testKeySetRemove() {
        var m = makeMap();
        m.putAll(Map.of(0, "zero", 1, "one", 2, "two",
                3, "three", 4, "four", 5, "five"));
        m.keySet().removeIf(n -> n % 2 == 0);
        assertEquals(3, m.size());
        assertEquals("{1=one, 3=three, 5=five}", m.toString());
        m.keySet().remove(3);
        assertEquals("{1=one, 5=five}", m.toString());
    }

    @Test
    public void testClear() {
        var m = makeMap();
        assertEquals(3, m.size());
        m.clear();
        assertEquals(0, m.size());
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

    @Test
    public void testMapExpansion() {
        var m = new IndexMap<Integer, String>(indexer, 3);
        m.put(20, "20");
        Assert.assertEquals("20", m.get(20));
    }

    @Test(expected = NullPointerException.class)
    public void testPutNull() {
        var m = makeMap();
        m.put(1, null);
    }
}
