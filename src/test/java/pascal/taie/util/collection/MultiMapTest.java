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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MultiMapTest {

    @Test
    void testPut() {
        MultiMap<Integer, Integer> m = Maps.newMultiMap();
        m.put(1, 1);
        m.put(1, 2);
        m.put(1, 3);
        assertTrue(m.put(2, 1));
        assertFalse(m.put(2, 1));
        assertFalse(m.put(2, 1));
        assertEquals(4, m.size());
    }

    @Test
    void testPutAll1() {
        MultiMap<Integer, Integer> m = Maps.newMultiMap();
        assertTrue(m.putAll(1, Set.of(6, 7, 8)));
        assertTrue(m.putAll(2, Set.of(1)));
        assertFalse(m.putAll(3, Set.of()));
        assertEquals(4, m.size());
    }

    @Test
    void testPutAll2() {
        MultiMap<Integer, Integer> m1 = Maps.newMultiMap();
        m1.putAll(1, Set.of(6, 7, 8));
        m1.putAll(2, Set.of(1));

        MultiMap<Integer, Integer> m2 = Maps.newMultiMap();
        assertTrue(m2.putAll(m1));
        assertEquals(4, m2.size());
    }

    @Test
    void testRemove() {
        MultiMap<Integer, Integer> m = Maps.newMultiMap();
        m.putAll(1, Set.of(555, 888, 666));
        m.putAll(2, Set.of(777));
        m.remove(1, 666);
        m.remove(2, 777);
        assertEquals(2, m.size());
    }

    @Test
    void testRemoveAll() {
        MultiMap<Integer, Integer> m = Maps.newMultiMap();
        m.putAll(1, Set.of(314, 159, 265));
        m.putAll(2, Set.of(3));
        assertTrue(m.removeAll(1));
        assertTrue(m.removeAll(2, Set.of(3, 5, 7)));
        assertTrue(m.isEmpty());
        assertFalse(m.containsKey(2));

        m.putAll(1, Set.of(314, 159, 265));
        m.removeAll(1, Set.of(314, 159, 265));
        assertFalse(m.containsKey(1));
    }

    @Test
    void testGet() {
        MultiMap<Integer, Integer> m = Maps.newMultiMap();
        m.putAll(1, Set.of(314, 159, 265));
        m.putAll(2, Set.of(3));
        assertEquals(3, m.get(1).size());
        assertEquals(1, m.get(2).size());
        assertEquals(0, m.get(3).size());
    }

    @Test
    void testContains() {
        MultiMap<Integer, Integer> m = Maps.newMultiMap();
        m.putAll(1, Set.of(314, 159, 265));
        m.putAll(2, Set.of(3));

        assertTrue(m.contains(1, 314));
        assertTrue(m.containsKey(2));
        assertTrue(m.containsValue(3));

        assertFalse(m.contains(1, 2333));
        assertFalse(m.containsKey(2333));
        assertFalse(m.containsValue(2333));

        m.remove(2, 3);
        assertFalse(m.containsKey(2));
    }

    @Test
    void testEquals() {
        MultiMap<Integer, Integer> m1 = Maps.newMultiMap();
        m1.putAll(1, Set.of(314, 159, 265));
        m1.putAll(2, Set.of(3));

        MultiMap<Integer, Integer> m2 = Maps.newMultiMap();
        m2.putAll(m1);
        assertEquals(m1, m2);
        m2.remove(1, 314);
        assertNotEquals(m1, m2);
        m2.put(1, 314);
        assertEquals(m1, m2);
        m2.put(1, 3);
        assertNotEquals(m1, m2);

        m1.clear();
        assertNotEquals(m1, m2);
        m2.clear();
        assertEquals(m1, m2);
    }

    @Test
    void testEntrySet() {
        MultiMap<Integer, Integer> m = Maps.newMultiMap();
        var entrySet = m.entrySet();
        m.put(1, 1);
        m.putAll(2, Set.of(777, 888, 999));
        m.putAll(3, Set.of(987, 555));
        assertEquals(6, entrySet.size());
        m.removeAll(2);
        assertEquals(3, entrySet.size());
        m.clear();
        assertTrue(entrySet.isEmpty());
    }

    @Test
    void testValues() {
        MultiMap<Integer, Integer> m = Maps.newMultiMap();
        var values = m.values();
        m.put(1, 1);
        m.putAll(2, Set.of(777, 888, 999));
        m.putAll(3, Set.of(987, 555));
        assertEquals(6, values.size());
        m.removeAll(2);
        assertEquals(3, values.size());
        m.clear();
        assertTrue(values.isEmpty());
    }

    @Test
    void testUnmodifiableMultiple() {
        MultiMap<Integer, Integer> m = Maps.newMultiMap();
        m.put(1, 1);
        m.putAll(2, Set.of(777, 888, 999));
        m.putAll(3, Set.of(987, 555));
        MultiMap<Integer, Integer> um = Maps.unmodifiableMultiMap(m);
        assertEquals(m.get(2), um.get(2));
        assertEquals(m, um);
        m.put(333, 666);
        assertEquals(m.get(333), um.get(333));
    }

    @Test
    void testUnmodifiableMultiplePut() {
        assertThrows(UnsupportedOperationException.class, () -> {
            MultiMap<Integer, Integer> um = Maps.unmodifiableMultiMap(Maps.newMultiMap());
            um.put(1, 2);
        });
    }

    @Test
    void testUnmodifiableMultipleRemove() {
        assertThrows(UnsupportedOperationException.class, () -> {
            MultiMap<Integer, Integer> um = Maps.unmodifiableMultiMap(Maps.newMultiMap());
            um.remove(1, 2);
        });
    }

    @Test
    void testMultiMapCollector() {
        MultiMap<Integer, Integer> m1 = Stream.of(
                new Pair<>(1, 1),
                new Pair<>(1, 2),
                new Pair<>(1, 3),
                new Pair<>(2, 4)
        ).collect(MultiMapCollector.get(Pair::first, Pair::second));
        MultiMap<Integer, Integer> m2 = Maps.newMultiMap();
        m2.putAll(1, Set.of(1, 2, 3));
        m2.put(2, 4);
        assertEquals(m1, m2);
    }

    @Test
    void testSerializable() {
        MultiMap<Integer, String> map1 = Maps.newMultiMap();
        map1.put(1, "x");
        map1.put(1, "xx");
        map1.put(2, "y");
        map1.put(3, "z");
        map1.put(3, "zz");
        MultiMap<Integer, String> map2 = SerializationUtils.serializedCopy(map1);
        assertEquals(map1, map2);
        map1.put(3, "zzz");
        map2.put(3, "zzz");
        assertEquals(map1, map2);
    }
}
