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
