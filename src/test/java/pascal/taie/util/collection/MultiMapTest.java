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
import pascal.taie.util.SerializationUtils;

import java.util.Set;
import java.util.stream.Stream;

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

    @Test
    public void testEntrySet() {
        MultiMap<Integer, Integer> m = Maps.newMultiMap();
        var entrySet = m.entrySet();
        m.put(1, 1);
        m.putAll(2, Set.of(777, 888, 999));
        m.putAll(3, Set.of(987, 555));
        Assert.assertEquals(6, entrySet.size());
        m.removeAll(2);
        Assert.assertEquals(3, entrySet.size());
        m.clear();
        Assert.assertTrue(entrySet.isEmpty());
    }

    @Test
    public void testValues() {
        MultiMap<Integer, Integer> m = Maps.newMultiMap();
        var values = m.values();
        m.put(1, 1);
        m.putAll(2, Set.of(777, 888, 999));
        m.putAll(3, Set.of(987, 555));
        Assert.assertEquals(6, values.size());
        m.removeAll(2);
        Assert.assertEquals(3, values.size());
        m.clear();
        Assert.assertTrue(values.isEmpty());
    }

    @Test
    public void testUnmodifiableMultiple() {
        MultiMap<Integer, Integer> m = Maps.newMultiMap();
        m.put(1, 1);
        m.putAll(2, Set.of(777, 888, 999));
        m.putAll(3, Set.of(987, 555));
        MultiMap<Integer, Integer> um = Maps.unmodifiableMultiMap(m);
        Assert.assertEquals(m.get(2), um.get(2));
        Assert.assertEquals(m, um);
        m.put(333, 666);
        Assert.assertEquals(m.get(333), um.get(333));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnmodifiableMultiplePut() {
        MultiMap<Integer, Integer> um = Maps.unmodifiableMultiMap(Maps.newMultiMap());
        um.put(1, 2);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnmodifiableMultipleRemove() {
        MultiMap<Integer, Integer> um = Maps.unmodifiableMultiMap(Maps.newMultiMap());
        um.remove(1, 2);
    }

    @Test
    public void testMultiMapCollector() {
        MultiMap<Integer, Integer> m1 = Stream.of(
                new Pair<>(1, 1),
                new Pair<>(1, 2),
                new Pair<>(1, 3),
                new Pair<>(2, 4)
        ).collect(MultiMapCollector.get(Pair::first, Pair::second));
        MultiMap<Integer, Integer> m2 = Maps.newMultiMap();
        m2.putAll(1, Set.of(1, 2, 3));
        m2.put(2, 4);
        Assert.assertEquals(m1, m2);
    }

    @Test
    public void testSerializable() {
        MultiMap<Integer, String> map1 = Maps.newMultiMap();
        map1.put(1, "x");
        map1.put(1, "xx");
        map1.put(2, "y");
        map1.put(3, "z");
        map1.put(3, "zz");
        MultiMap<Integer, String> map2 = SerializationUtils.serializedCopy(map1);
        Assert.assertEquals(map1, map2);
        map1.put(3, "zzz");
        map2.put(3, "zzz");
        Assert.assertEquals(map1, map2);
    }
}
