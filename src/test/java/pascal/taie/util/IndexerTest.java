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

package pascal.taie.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IndexerTest {

    @Test
    void testInit() {
        Indexer<String> i = new SimpleIndexer<>(List.of("aaa", "xxx", "yyy"));
        assertEquals(0, i.getIndex("aaa"));
        assertEquals(0, i.getIndex("aaa"));
        assertEquals(1, i.getIndex("xxx"));
        assertEquals(2, i.getIndex("yyy"));
        assertEquals("aaa", i.getObject(0));
        assertEquals("xxx", i.getObject(1));
        assertEquals("yyy", i.getObject(2));
    }

    @Test
    void testGet() {
        Indexer<String> i = new SimpleIndexer<>();
        assertEquals(0, i.getIndex("0"));
        assertEquals(0, i.getIndex("0"));
        assertEquals(1, i.getIndex("1"));
        assertEquals(2, i.getIndex("2"));
    }

    @Test
    void testGetNull() {
        assertThrows(NullPointerException.class, () ->
                new SimpleIndexer<>().getIndex(null));
    }

    @Test
    void testAbsentId() {
        assertThrows(IllegalArgumentException.class, () ->
                new SimpleIndexer<>().getObject(666));
    }
}
