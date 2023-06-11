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

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ConstantConditions")
public class TwoKeyMapTest {

    @Test
    public void testPut() {
        TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
        m.put(1, 1, 1);
        m.put(1, 2, 2);
        m.put(3, 4, 12);
        Assert.assertNotNull(m.put(1, 1, 1));
        Assert.assertNull(m.put(3, 6, 18));
        Assert.assertNotNull(m.put(3, 6, 18));
        Assert.assertEquals(4, m.size());
    }

    @Test
    public void testPutAll1() {
        TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
        m.putAll(1, Map.of(1, 1, 2, 2, 3, 3, 4, 4));
        m.putAll(2, Map.of(1, 2, 2, 4, 3, 6, 4, 8));
        Assert.assertEquals(8, m.size());
    }

    @Test
    public void testPutAll2() {
        TwoKeyMap<Integer, Integer, Integer> m1 = Maps.newTwoKeyMap();
        m1.putAll(1, Map.of(1, 1, 2, 2, 3, 3, 4, 4));
        m1.putAll(2, Map.of(1, 2, 2, 4, 3, 6, 4, 8));

        TwoKeyMap<Integer, Integer, Integer> m2 = Maps.newTwoKeyMap();
        m2.putAll(3, Map.of(1, 3, 2, 6, 3, 9, 4, 12, 5, 15));
        m1.putAll(m2);
        Assert.assertEquals(13, m1.size());
    }

    @Test
    public void testRemove() {
        TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
        m.put(1, 1, 1);
        m.put(1, 2, 2);
        m.put(3, 4, 12);
        Assert.assertEquals(3, m.size());
        Assert.assertEquals(1, (int) m.remove(1, 1));
        Assert.assertEquals(2, m.size());
        Assert.assertNull(m.remove(1, 1));
        Assert.assertEquals(2, m.size());
        Assert.assertEquals(2, (int) m.remove(1, 2));
        Assert.assertEquals(1, m.size());
        Assert.assertEquals(12, (int) m.remove(3, 4));
        Assert.assertEquals(0, m.size());
        Assert.assertTrue(m.isEmpty());
    }

    @Test
    public void testRemoveAll() {
        TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
        m.putAll(1, Map.of(1, 1, 2, 2, 3, 3, 4, 4));
        m.putAll(2, Map.of(1, 2, 2, 4, 3, 6, 4, 8));
        m.putAll(3, Map.of(1, 3, 2, 6, 3, 9, 4, 12, 5, 15));

        Assert.assertFalse(m.removeAll(4));
        Assert.assertTrue(m.removeAll(1));
        Assert.assertEquals(9, m.size());
        Assert.assertTrue(m.removeAll(2));
        Assert.assertEquals(5, m.size());
        Assert.assertTrue(m.removeAll(3));
        Assert.assertEquals(0, m.size());
        Assert.assertFalse(m.removeAll(1));
    }

    @Test
    public void testGet() {
        TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
        m.putAll(1, Map.of(1, 1, 2, 2, 3, 3, 4, 4));
        m.putAll(2, Map.of(1, 2, 2, 4, 3, 6, 4, 8));
        m.putAll(3, Map.of(1, 3, 2, 6, 3, 9, 4, 12, 5, 15));

        Assert.assertEquals(1, (int) m.get(1, 1));
        Assert.assertEquals(8, (int) m.get(2, 4));
        Assert.assertEquals(12, (int) m.get(3, 4));
        Assert.assertNull(m.get(10, 10));
    }

    @Test
    public void testContainsKey() {
        TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
        m.putAll(1, Map.of(1, 1, 2, 2, 3, 3, 4, 4));
        m.putAll(2, Map.of(1, 2, 2, 4, 3, 6, 4, 8));

        Assert.assertTrue(m.containsKey(1, 1));
        Assert.assertTrue(m.containsKey(2));
        Assert.assertTrue(m.containsValue(8));

        Assert.assertFalse(m.containsKey(1, 2333));
        Assert.assertFalse(m.containsKey(2333));
        Assert.assertFalse(m.containsValue(2333));

        m.remove(1, 1);
        Assert.assertFalse(m.containsKey(1, 1));
    }

    @Test
    public void testEquals() {
        TwoKeyMap<Integer, Integer, Integer> m1 = Maps.newTwoKeyMap();
        m1.putAll(10, Map.of(1, 10, 2, 20, 3, 30));
        m1.putAll(11, Map.of(4, 44, 5, 55, 6, 66));

        TwoKeyMap<Integer, Integer, Integer> m2 = Maps.newTwoKeyMap();
        m2.putAll(m1);
        Assert.assertEquals(m1, m2);
        m2.remove(11, 4);
        Assert.assertNotEquals(m1, m2);
        m2.put(11, 4, 44);
        Assert.assertEquals(m1, m2);
        m2.put(12, 5, 60);
        Assert.assertNotEquals(m1, m2);

        m1.clear();
        Assert.assertNotEquals(m1, m2);
        m2.clear();
        Assert.assertEquals(m1, m2);
    }

    @Test
    public void testEntrySet() {
        TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
        var entrySet = m.entrySet();
        m.put(1, 1, 1);
        m.putAll(10, Map.of(1, 10, 2, 20, 3, 30));
        m.putAll(11, Map.of(4, 44, 5, 55, 6, 66));
        Assert.assertEquals(7, entrySet.size());
        m.removeAll(10);
        Assert.assertEquals(4, entrySet.size());
        m.clear();
        Assert.assertTrue(entrySet.isEmpty());
    }

    @Test
    public void testTwoKeySet() {
        TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
        var twoKeySet = m.keyPairSet();
        m.put(1, 1, 1);
        m.putAll(10, Map.of(1, 10, 2, 20, 3, 30));
        m.putAll(11, Map.of(4, 44, 5, 55, 6, 66));
        Assert.assertEquals(7, twoKeySet.size());
        m.removeAll(10);
        Assert.assertEquals(4, twoKeySet.size());
        m.clear();
        Assert.assertTrue(twoKeySet.isEmpty());
    }

    @Test
    public void testValues() {
        TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
        var values = m.values();
        m.put(1, 1, 1);
        m.putAll(10, Map.of(1, 10, 2, 20, 3, 30));
        m.putAll(11, Map.of(4, 44, 5, 55, 6, 66));
        Assert.assertEquals(7, values.size());
        m.removeAll(10);
        Assert.assertEquals(4, values.size());
        m.clear();
        Assert.assertTrue(values.isEmpty());
    }

    @Test
    public void testGetOrDefault() {
        TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
        Assert.assertEquals(777, (int) m.getOrDefault(1, 1, 777));
        m.put(1, 1, 1);
        Assert.assertEquals(1, (int) m.getOrDefault(1, 1, 777));
    }

    @Test
    public void testComputeIfAbsent() {
        TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
        Assert.assertEquals(777,
                (int) m.computeIfAbsent(1, 1, (k1, k2) -> 777));
        Assert.assertEquals(777, (int) m.get(1, 1));
    }

    @Test(expected = NullPointerException.class)
    public void testPutNull1() {
        TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
        m.put(1, 2, null);
    }

    @Test(expected = NullPointerException.class)
    public void testPutNull2() {
        TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
        Map<Integer, Integer> map = new HashMap<>();
        map.put(1, 1);
        map.put(2, null);
        m.putAll(1, map);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testModifyViaGetMap() {
        TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
        m.put(1, 1, 1);
        m.get(1).put(2, 2);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testModifyViaEntrySet() {
        TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
        m.entrySet().add(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testModifyViaEntrySetIterator() {
        TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
        m.put(1, 1, 1);
        var it = m.entrySet().iterator();
        it.next();
        it.remove();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testModifyViaKeySet() {
        TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
        m.keySet().add(1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testModifyViaValues() {
        TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
        m.values().add(1);
    }


    @Test
    public void testSerializable() {
        TwoKeyMap<Integer, Integer, Integer> map1 = Maps.newTwoKeyMap();
        map1.put(1, 1, 1);
        map1.put(1, 2, 2);
        map1.put(3, 4, 12);
        TwoKeyMap<Integer, Integer, Integer> map2 = SerializationUtils.serializedCopy(map1);
        Assert.assertEquals(map1, map2);
    }
}
