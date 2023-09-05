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

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TwoKeyMultiMapTest {

    @Test
    void testPut() {
        TwoKeyMultiMap<Integer, Integer, Integer> m = Maps.newTwoKeyMultiMap();
        m.put(1, 1, 2);
        m.put(1, 1, 0);
        m.put(1, 1, 1);
        m.put(1, 2, 2);
        m.put(3, 4, 12);

        assertFalse(m.put(1, 1, 1));
        assertTrue(m.put(6, 6, 36));
        assertFalse(m.put(1, 1, 2));
        assertEquals(6, m.size());
    }

    @Test
    void testRemove() {
        TwoKeyMultiMap<Integer, Integer, Integer> m = Maps.newTwoKeyMultiMap();
        m.put(7, 3, 10);
        m.put(7, 3, 4);
        m.put(7, 3, 21);
        m.put(7, 3, 2);
        m.put(1, 2, 3);
        m.put(1, 2, -1);
        m.put(1, 2, 2);
        m.put(1, 2, 0);
        m.put(3, 4, 7);

        assertEquals(9, m.size());
        assertTrue(m.remove(7, 3, 10));
        assertEquals(8, m.size());
        assertFalse(m.remove(7, 3, 10));
        assertEquals(8, m.size());
        assertTrue(m.remove(3, 4, 7));
        assertEquals(7, m.size());
    }

    @Test
    void testRemoveAll() {
        TwoKeyMultiMap<Integer, Integer, Integer> m = Maps.newTwoKeyMultiMap();
        m.put(7, 3, 10);
        m.put(7, 3, 4);
        m.put(7, 3, 21);
        m.put(7, 3, 2);
        m.put(1, 2, 3);
        m.put(1, 2, -1);
        m.put(1, 2, 2);
        m.put(1, 2, 0);
        m.put(3, 4, 7);

        assertFalse(m.removeAll(7, 4));
        assertTrue(m.removeAll(7, 3));
        assertEquals(5, m.size());
        assertTrue(m.removeAll(1, 2));
        assertFalse(m.removeAll(1, 2));
        assertEquals(1, m.size());
        assertTrue(m.removeAll(3, 4));
        assertTrue(m.isEmpty());
    }

    @Test
    void testGet() {
        TwoKeyMultiMap<Integer, Integer, Integer> m = Maps.newTwoKeyMultiMap();
        m.put(7, 3, 10);
        m.put(7, 3, 4);
        m.put(7, 3, 21);
        m.put(7, 3, 2);
        m.put(1, 2, 3);
        m.put(1, 2, -1);
        m.put(1, 2, 2);
        m.put(1, 2, 0);
        m.put(3, 4, 7);

        assertEquals(Set.of(10, 4, 21, 2), m.get(7, 3));
        assertEquals(Set.of(3, -1, 2, 0), m.get(1, 2));
        assertEquals(Set.of(7), m.get(3, 4));
        assertTrue(m.get(10, 10).isEmpty());
    }

    @Test
    void testContains() {
        TwoKeyMultiMap<Integer, Integer, Integer> m = Maps.newTwoKeyMultiMap();
        m.put(7, 3, 10);
        m.put(7, 3, 4);
        m.put(7, 3, 21);
        m.put(7, 3, 2);
        m.put(1, 2, 3);
        m.put(1, 2, -1);
        m.put(1, 2, 2);
        m.put(1, 2, 0);
        m.put(3, 4, 7);

        assertTrue(m.containsKey(7, 3));
        assertTrue(m.containsKey(7));
        assertTrue(m.containsValue(10));

        assertFalse(m.containsKey(2333, 1));
        assertFalse(m.containsKey(2333));
        assertFalse(m.containsValue(2333));

        m.removeAll(1, 2);
        assertFalse(m.containsKey(1, 2));
    }

    @Test
    void testEquals() {
        TwoKeyMultiMap<Integer, Integer, Integer> m1 = Maps.newTwoKeyMultiMap();
        m1.put(7, 3, 10);
        m1.put(7, 3, 4);
        m1.put(7, 3, 21);
        m1.put(7, 3, 2);
        m1.put(1, 2, 3);
        m1.put(1, 2, -1);
        m1.put(1, 2, 2);
        m1.put(1, 2, 0);
        m1.put(3, 4, 7);

        TwoKeyMultiMap<Integer, Integer, Integer> m2 = Maps.newTwoKeyMultiMap();
        m2.put(3, 4, 7);
        m2.put(1, 2, 0);
        m2.put(1, 2, 2);
        m2.put(1, 2, -1);
        m2.put(1, 2, 3);
        m2.put(7, 3, 2);
        m2.put(7, 3, 21);
        m2.put(7, 3, 4);
        m2.put(7, 3, 10);

        assertEquals(m1, m2);
        m2.remove(1, 2, 3);
        assertNotEquals(m1, m2);
        m2.put(1, 2, 3);
        assertEquals(m1, m2);
        m2.put(3, 4, -1);
        assertNotEquals(m1, m2);

        m1.clear();
        assertNotEquals(m1, m2);
        m2.clear();
        assertEquals(m1, m2);
    }

    @Test
    void testEntrySet() {
        TwoKeyMultiMap<Integer, Integer, Integer> m = Maps.newTwoKeyMultiMap();
        var entrySet = m.entrySet();
        m.put(7, 3, 10);
        m.put(7, 3, 4);
        m.put(7, 3, 21);
        m.put(7, 3, 2);
        m.put(1, 2, 3);
        m.put(1, 2, -1);
        m.put(1, 2, 2);
        m.put(1, 2, 0);
        m.put(3, 4, 7);

        assertEquals(9, entrySet.size());
        m.removeAll(7, 3);
        assertEquals(5, entrySet.size());
        m.clear();
        assertTrue(entrySet.isEmpty());

    }

    @Test
    void testTwoKeySet() {
        TwoKeyMultiMap<Integer, Integer, Integer> m = Maps.newTwoKeyMultiMap();
        var twoKeySet = m.twoKeySet();
        m.put(7, 3, 10);
        m.put(7, 3, 4);
        m.put(7, 3, 21);
        m.put(7, 3, 2);
        m.put(1, 2, 3);
        m.put(1, 2, -1);
        m.put(1, 2, 2);
        m.put(1, 2, 0);
        m.put(3, 4, 7);

        assertEquals(3, twoKeySet.size());
        m.removeAll(1, 2);
        assertEquals(2, twoKeySet.size());
        m.clear();
        assertTrue(twoKeySet.isEmpty());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    void testPutNull() {
        TwoKeyMultiMap<Integer, Integer, Integer> m = Maps.newTwoKeyMultiMap();
        assertThrows(NullPointerException.class, () -> m.put(null, 2, 3));
        assertThrows(NullPointerException.class, () -> m.put(1, null, 3));
        assertThrows(NullPointerException.class, () -> m.put(1, 2, null));
        assertTrue(m.isEmpty());
    }

    @Test
    void testModifyViaGetSet() {
        assertThrows(UnsupportedOperationException.class, () -> {
            TwoKeyMultiMap<Integer, Integer, Integer> m = Maps.newTwoKeyMultiMap();
            m.put(1, 2, 3);
            m.get(1, 2).add(2);
        });
    }

    @Test
    void testModifyViaEntrySet() {
        assertThrows(UnsupportedOperationException.class, () -> {
            TwoKeyMultiMap<Integer, Integer, Integer> m = Maps.newTwoKeyMultiMap();
            m.entrySet().add(new TwoKeyMap.Entry<>(1, 2, 3));
        });
    }

    @Test
    void testModifyViaEntrySetIterator() {
        assertThrows(UnsupportedOperationException.class, () -> {
            TwoKeyMultiMap<Integer, Integer, Integer> m = Maps.newTwoKeyMultiMap();
            m.put(1, 2, 3);
            var it = m.entrySet().iterator();
            it.next();
            it.remove();
        });
    }

    @Test
    void testModifyViaKeySet() {
        assertThrows(UnsupportedOperationException.class, () -> {
            TwoKeyMultiMap<Integer, Integer, Integer> m = Maps.newTwoKeyMultiMap();
            m.twoKeySet().add(new Pair<>(1, 2));
        });
    }

    @Test
    void testSerializable() {
        TwoKeyMultiMap<Integer, Integer, Integer> map1 = Maps.newTwoKeyMultiMap();
        map1.put(1, 1, 1);
        map1.put(1, 2, 2);
        map1.put(3, 4, 12);
        TwoKeyMultiMap<Integer, Integer, Integer> map2 = SerializationUtils.serializedCopy(map1);
        assertEquals(map1, map2);
    }
}
