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

public class MapperTest {

    @Test
    public void testInit() {
        ObjectIdMapper<String> m = new SimpleMapper<>(List.of("aaa", "xxx", "yyy"));
        assertEquals(0, m.getId("aaa"));
        assertEquals(0, m.getId("aaa"));
        assertEquals(1, m.getId("xxx"));
        assertEquals(2, m.getId("yyy"));
        assertEquals("aaa", m.getObject(0));
        assertEquals("xxx", m.getObject(1));
        assertEquals("yyy", m.getObject(2));
    }

    @Test
    public void testGet() {
        ObjectIdMapper<String> m = new SimpleMapper<>();
        assertEquals(0, m.getId("0"));
        assertEquals(0, m.getId("0"));
        assertEquals(1, m.getId("1"));
        assertEquals(2, m.getId("2"));
    }

    @Test(expected = NullPointerException.class)
    public void testGetNull() {
        ObjectIdMapper<String> m = new SimpleMapper<>();
        m.getId(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAbsentId() {
        ObjectIdMapper<String> m = new SimpleMapper<>();
        m.getObject(666);
    }
}
