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
        assertFalse(of(1).contains(of(1, 2, 3)));

        BitSet s = of(1, 2, 3);
        int cardinality = s.cardinality();
        assertFalse(s.contains(of(11111)));
        assertEquals(cardinality, s.cardinality());
        s = of(1, 2, 3, 11111);
        cardinality = s.cardinality();
        assertTrue(s.contains(of(11111)));
        assertEquals(cardinality, s.cardinality());
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
        BitSet s = of(1, 2, 3, 6666);
        assertFalse(s.andNot(of()));
        assertTrue(s.andNot(of(1, 2, 3, 6666)));
        assertTrue(s.isEmpty());
        s = of(1, 2, 3, 6666);
        assertTrue(s.andNot(of(6666)));
        assertFalse(s.isEmpty());

        s = of();
        assertTrue(s.or(of(1, 1, 1)));
        assertFalse(s.andNot(of(2)));
        assertEquals(1, s.cardinality());
        assertTrue(s.or(of(1, 333, 5555, 777, 99999)));
        assertFalse(s.andNot(of(222, 444, 666, 888, 10000)));
        assertEquals(5, s.cardinality());
        assertTrue(s.andNot(of(333, 777, 99999)));
        assertEquals(2, s.cardinality());
        System.out.println(s);
    }

    @Test
    public void testOr() {
        BitSet s = of(1, 2, 3);
        assertFalse(s.or(of(1, 2, 3)));
        assertFalse(s.or(of(1)));
        assertEquals(3, s.cardinality());
        assertTrue(s.or(of(1, 11111, 22222, 333333)));
        assertFalse(s.or(of(11111, 22222, 333333)));
        assertEquals(6, s.cardinality());
        System.out.println(s);
    }

    @Test
    public void testClearOr() {
        BitSet s = of(1, 555, 66666);
        BitSet s2 = of(1, 555, 66666);
        s2.clear(66666);
        assertFalse(s.or(s2));
        assertEquals(3, s.cardinality());

        s = of();
        s2.clear(1);
        s2.clear(555);
        assertFalse(s.or(s2));
        assertTrue(s.isEmpty());
    }

    @Test
    public void testXor() {
        BitSet s = of(1, 2, 300);
        assertTrue(s.xor(s));
        assertTrue(s.isEmpty());
        s = of(1, 444, 7777);
        assertTrue(s.xor(of(1, 444, 7777)));
        assertTrue(s.isEmpty());
        s = of(22, 333, 4444, 55555);
        assertTrue(s.xor(of(333, 2222, 55555, 66666)));
        assertEquals(4, s.cardinality());
        System.out.println(s);
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
