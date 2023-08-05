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

import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("ALL")
abstract class AbstractSetTest {

    protected abstract <E> Set<E> newSet();

    @Test
    void testAdd() {
        Set<String> set = newSet();
        set.add("a");
        set.add("a");
        set.add("b");
        set.add("c");
        assertEquals(3, set.size());
    }

    @Test
    void testAddNull() {
        assertThrows(NullPointerException.class, () -> {
            Set<String> set = newSet();
            set.add("a");
            set.add(null);
            set.add("b");
        });
    }

    void testAddNElements(Set<Integer> set, int n) {
        set.clear();
        for (int i = 0; i < n; ++i) {
            set.add(i);
        }
        assertEquals(n, set.size());
    }

    @Test
    void testAddAll() {
        Set<String> set = newSet();
        set.addAll(Arrays.asList("a", "a", "b", "c", "c"));
        assertEquals(3, set.size());
    }

    @Test
    void testRemove() {
        Set<String> set = newSet();
        set.add("a");
        set.add("b");
        set.add("c");
        assertEquals(3, set.size());
        set.remove("x");
        assertEquals(3, set.size());
        set.remove("a");
        assertEquals(2, set.size());
        set.remove("b");
        assertEquals(1, set.size());
    }

    @Test
    void testEmpty() {
        Set<String> set = newSet();
        assertEquals(0, set.size());
        set.remove("x");
        assertEquals(0, set.size());
    }

    @Test
    void testSerializable() {
        Set<String> set1 = newSet();
        set1.add("a");
        set1.add("b");
        set1.add("c");
        Set<String> set2 = SerializationUtils.serializedCopy(set1);
        assertEquals(set1, set2);
        set1.add("d");
        set2.add("d");
        assertEquals(set1, set2);
    }
}
