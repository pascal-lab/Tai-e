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

package pascal.taie.util;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class IndexerTest {

    @Test
    public void testInit() {
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
    public void testGet() {
        Indexer<String> i = new SimpleIndexer<>();
        assertEquals(0, i.getIndex("0"));
        assertEquals(0, i.getIndex("0"));
        assertEquals(1, i.getIndex("1"));
        assertEquals(2, i.getIndex("2"));
    }

    @Test(expected = NullPointerException.class)
    public void testGetNull() {
        new SimpleIndexer<>().getIndex(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAbsentId() {
        new SimpleIndexer<>().getObject(666);
    }
}
