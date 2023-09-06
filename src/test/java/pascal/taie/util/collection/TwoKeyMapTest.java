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

import org.junit.jupiter.api.Test;
import pascal.taie.util.SerializationUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("ConstantConditions")
public class TwoKeyMapTest {

    @Test
    void testPut() {
        TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
        m.put(1, 1, 1);
        m.put(1, 2, 2);
        m.put(3, 4, 12);
        assertNotNull(m.put(1, 1, 1));
        assertNull(m.put(3, 6, 18));
        assertNotNull(m.put(3, 6, 18));
        assertEquals(4, m.size());
    }

    @Test
    void testPutAll1() {
        TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
        m.putAll(1, Map.of(1, 1, 2, 2, 3, 3, 4, 4));
        m.putAll(2, Map.of(1, 2, 2, 4, 3, 6, 4, 8));
        assertEquals(8, m.size());
    }

    @Test
    void testPutAll2() {
        TwoKeyMap<Integer, Integer, Integer> m1 = Maps.newTwoKeyMap();
        m1.putAll(1, Map.of(1, 1, 2, 2, 3, 3, 4, 4));
        m1.putAll(2, Map.of(1, 2, 2, 4, 3, 6, 4, 8));

        TwoKeyMap<Integer, Integer, Integer> m2 = Maps.newTwoKeyMap();
        m2.putAll(3, Map.of(1, 3, 2, 6, 3, 9, 4, 12, 5, 15));
        m1.putAll(m2);
        assertEquals(13, m1.size());
    }

    @Test
    void testRemove() {
        TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
        m.put(1, 1, 1);
        m.put(1, 2, 2);
        m.put(3, 4, 12);
        assertEquals(3, m.size());
        assertEquals(1, (int) m.remove(1, 1));
        assertEquals(2, m.size());
        assertNull(m.remove(1, 1));
        assertEquals(2, m.size());
        assertEquals(2, (int) m.remove(1, 2));
        assertEquals(1, m.size());
        assertEquals(12, (int) m.remove(3, 4));
        assertEquals(0, m.size());
        assertTrue(m.isEmpty());
    }

    @Test
    void testRemoveAll() {
        TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
        m.putAll(1, Map.of(1, 1, 2, 2, 3, 3, 4, 4));
        m.putAll(2, Map.of(1, 2, 2, 4, 3, 6, 4, 8));
        m.putAll(3, Map.of(1, 3, 2, 6, 3, 9, 4, 12, 5, 15));

        assertFalse(m.removeAll(4));
        assertTrue(m.removeAll(1));
        assertEquals(9, m.size());
        assertTrue(m.removeAll(2));
        assertEquals(5, m.size());
        assertTrue(m.removeAll(3));
        assertEquals(0, m.size());
        assertFalse(m.removeAll(1));
    }

    @Test
    void testGet() {
        TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
        m.putAll(1, Map.of(1, 1, 2, 2, 3, 3, 4, 4));
        m.putAll(2, Map.of(1, 2, 2, 4, 3, 6, 4, 8));
        m.putAll(3, Map.of(1, 3, 2, 6, 3, 9, 4, 12, 5, 15));

        assertEquals(1, (int) m.get(1, 1));
        assertEquals(8, (int) m.get(2, 4));
        assertEquals(12, (int) m.get(3, 4));
        assertNull(m.get(10, 10));
    }

    @Test
    void testContainsKey() {
        TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
        m.putAll(1, Map.of(1, 1, 2, 2, 3, 3, 4, 4));
        m.putAll(2, Map.of(1, 2, 2, 4, 3, 6, 4, 8));

        assertTrue(m.containsKey(1, 1));
        assertTrue(m.containsKey(2));
        assertTrue(m.containsValue(8));

        assertFalse(m.containsKey(1, 2333));
        assertFalse(m.containsKey(2333));
        assertFalse(m.containsValue(2333));

        m.remove(1, 1);
        assertFalse(m.containsKey(1, 1));
    }

    @Test
    void testEquals() {
        TwoKeyMap<Integer, Integer, Integer> m1 = Maps.newTwoKeyMap();
        m1.putAll(10, Map.of(1, 10, 2, 20, 3, 30));
        m1.putAll(11, Map.of(4, 44, 5, 55, 6, 66));

        TwoKeyMap<Integer, Integer, Integer> m2 = Maps.newTwoKeyMap();
        m2.putAll(m1);
        assertEquals(m1, m2);
        m2.remove(11, 4);
        assertNotEquals(m1, m2);
        m2.put(11, 4, 44);
        assertEquals(m1, m2);
        m2.put(12, 5, 60);
        assertNotEquals(m1, m2);

        m1.clear();
        assertNotEquals(m1, m2);
        m2.clear();
        assertEquals(m1, m2);
    }

    @Test
    void testEntrySet() {
        TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
        var entrySet = m.entrySet();
        m.put(1, 1, 1);
        m.putAll(10, Map.of(1, 10, 2, 20, 3, 30));
        m.putAll(11, Map.of(4, 44, 5, 55, 6, 66));
        assertEquals(7, entrySet.size());
        m.removeAll(10);
        assertEquals(4, entrySet.size());
        m.clear();
        assertTrue(entrySet.isEmpty());
    }

    @Test
    void testTwoKeySet() {
        TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
        var twoKeySet = m.twoKeySet();
        m.put(1, 1, 1);
        m.putAll(10, Map.of(1, 10, 2, 20, 3, 30));
        m.putAll(11, Map.of(4, 44, 5, 55, 6, 66));
        assertEquals(7, twoKeySet.size());
        m.removeAll(10);
        assertEquals(4, twoKeySet.size());
        m.clear();
        assertTrue(twoKeySet.isEmpty());
    }

    @Test
    void testValues() {
        TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
        var values = m.values();
        m.put(1, 1, 1);
        m.putAll(10, Map.of(1, 10, 2, 20, 3, 30));
        m.putAll(11, Map.of(4, 44, 5, 55, 6, 66));
        assertEquals(7, values.size());
        m.removeAll(10);
        assertEquals(4, values.size());
        m.clear();
        assertTrue(values.isEmpty());
    }

    @Test
    void testGetOrDefault() {
        TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
        assertEquals(777, (int) m.getOrDefault(1, 1, 777));
        m.put(1, 1, 1);
        assertEquals(1, (int) m.getOrDefault(1, 1, 777));
    }

    @Test
    void testComputeIfAbsent() {
        TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
        assertEquals(777,
                (int) m.computeIfAbsent(1, 1, (k1, k2) -> 777));
        assertEquals(777, (int) m.get(1, 1));
    }

    @Test
    void testPutNull1() {
        assertThrows(NullPointerException.class, () -> {
            TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
            m.put(1, 2, null);
        });
    }

    @Test
    void testPutNull2() {
        assertThrows(NullPointerException.class, () -> {
            TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
            Map<Integer, Integer> map = new HashMap<>();
            map.put(1, 1);
            map.put(2, null);
            m.putAll(1, map);
        });
    }

    @Test
    void testModifyViaGetMap() {
        assertThrows(UnsupportedOperationException.class, () -> {
            TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
            m.put(1, 1, 1);
            m.get(1).put(2, 2);
        });
    }

    @Test
    void testModifyViaEntrySet() {
        assertThrows(UnsupportedOperationException.class, () -> {
            TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
            m.entrySet().add(null);
        });
    }

    @Test
    void testModifyViaEntrySetIterator() {
        assertThrows(UnsupportedOperationException.class, () -> {
            TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
            m.put(1, 1, 1);
            var it = m.entrySet().iterator();
            it.next();
            it.remove();
        });
    }

    @Test
    void testModifyViaKeySet() {
        assertThrows(UnsupportedOperationException.class, () -> {
            TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
            m.keySet().add(1);
        });
    }

    @Test
    void testModifyViaValues() {
        assertThrows(UnsupportedOperationException.class, () -> {
            TwoKeyMap<Integer, Integer, Integer> m = Maps.newTwoKeyMap();
            m.values().add(1);
        });
    }


    @Test
    void testSerializable() {
        TwoKeyMap<Integer, Integer, Integer> map1 = Maps.newTwoKeyMap();
        map1.put(1, 1, 1);
        map1.put(1, 2, 2);
        map1.put(3, 4, 12);
        TwoKeyMap<Integer, Integer, Integer> map2 = SerializationUtils.serializedCopy(map1);
        assertEquals(map1, map2);
    }
}
