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

package pascal.taie.util.collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public abstract class BitSetTest {

    protected abstract BitSet of(int... indexes);
    
    @Test
    public void testSet() {
        BitSet s = of();
        assertTrue(s.set(1));
        assertFalse(s.set(1));
        assertTrue(s.set(10000));
        assertFalse(s.set(10000));
        assertEquals(2, s.cardinality());
        System.out.println(s);
    }

    @Test
    public void testClear() {
        BitSet s = of(1, 10000);
        assertEquals(2, s.cardinality());
        s.clear(1);
        assertEquals(1, s.cardinality());
        s.clear(10000);
        assertEquals(0, s.cardinality());
    }

    @Test
    public void testIntersects() {
        BitSet s1 = of();
        BitSet s2 = of();
        assertFalse(s1.intersects(s2));
        s2 = of(1, 233, 666);
        assertFalse(s1.intersects(s2));
        assertFalse(s2.intersects(s1));
        s1 = of(666, 777, 888);
        assertTrue(s1.intersects(s2));
        assertTrue(s2.intersects(s1));
        s1.clear(666);
        assertFalse(s1.intersects(s2));
    }

    @Test
    public void testContains() {
        assertTrue(of().contains(of()));
        assertTrue(of(1, 2, 3).contains(of()));
        assertTrue(of(1, 2, 3).contains(of(1)));
        assertTrue(of(1, 2, 3).contains(of(1, 2, 3)));
        assertFalse(of(1, 2, 3).contains(of(11111)));
        assertTrue(of(1, 2, 3, 11111).contains(of(11111)));
        assertFalse(of(1).contains(of(1, 2, 3)));
    }

    @Test
    public void testAnd() {
        BitSet s = of(1, 2, 3);
        assertFalse(s.and(of(1, 2, 3)));
        assertTrue(s.and(of(1)));
        assertEquals(1, s.cardinality());
        assertFalse(s.and(of(1, 11111, 22222, 33333)));
        assertTrue(s.and(of(11111, 22222, 33333)));
        assertTrue(s.isEmpty());
        System.out.println(s);
    }

    @Test
    public void testAndNot() {
        BitSet s = of(1, 2, 3);
        s.andNot(of(1, 2, 3));
        assertTrue(s.isEmpty());
        s.andNot(of(1, 2, 3));
        assertTrue(s.isEmpty());
        s.or(of(1, 1, 1));
        s.andNot(of(2));
        assertEquals(1, s.cardinality());
        System.out.println(s);
    }

    @Test
    public void testOr() {
        BitSet s = of(1, 2, 3);
        assertFalse(s.or(of(1, 2, 3)));
        assertFalse(s.or(of(1)));
        assertEquals(3, s.cardinality());
        assertTrue(s.or(of(1, 11111, 22222, 33333)));
        assertFalse(s.or(of(11111, 22222, 33333)));
        assertEquals(6, s.cardinality());
        System.out.println(s);
    }

    @Test
    public void testXor() {
        BitSet s = of(1, 2, 300);
        assertTrue(s.xor(s));
        assertTrue(s.isEmpty());
    }

    @Test
    public void testSetTo() {
        BitSet s = of(1, 2, 300);
        s.setTo(of());
        assertTrue(s.isEmpty());
        s.setTo(of(111, 222, 333));
        assertEquals(s, of(111, 222, 333));
        s.setTo(of(1));
        assertEquals(s, of(1));
        s.setTo(of(11111));
        assertEquals(s, of(11111));
    }
}
