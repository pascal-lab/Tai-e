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

package pascal.taie.analysis.dfa.fact;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class FactTest {

    @Test
    public void testToppedSetFact() {
        ToppedSetFact<Integer> top = new ToppedSetFact<>(true);
        ToppedSetFact<Integer> fact1 = top.duplicate();
        fact1.intersect(top);
        Assert.assertTrue(fact1.isTop());

        ToppedSetFact<Integer> fact2 = new ToppedSetFact<>(List.of(1, 2, 3));
        fact1.intersect(fact2);
        Assert.assertFalse(fact1.isTop());
        fact2.union(top);
        Assert.assertTrue(fact2.isTop());

        ToppedSetFact<Integer> fact3 = new ToppedSetFact<>(List.of(8, 9, 10));
        fact2.union(fact3);
        Assert.assertTrue(fact2.isTop());
        fact2.intersect(top);
        Assert.assertTrue(fact2.isTop());
        fact2.intersect(fact3);
        Assert.assertFalse(fact2.isTop());
    }

    @Test
    public void testUnionNormal() {
        // Union overlapped sets
        SetFact<String> f1 = newSetFact("x", "y");
        SetFact<String> f2 = newSetFact("y", "z");
        SetFact<String> f3 = f1.unionWith(f2);
        Assert.assertEquals(f3.size(), 3);

        // Union two disjoint sets
        f1 = newSetFact("a", "b");
        f2 = newSetFact("c", "d");
        f3 = f1.unionWith(f2);
        Assert.assertEquals(f3.size(), 4);

        // Union empty set
        f1 = newSetFact();
        f2 = newSetFact("xxx", "yyy");
        f3 = f1.unionWith(f2);
        Assert.assertEquals(f3.size(), 2);
    }
    
    @Test
    public void testIntersectNormal() {
        // Intersect overlapped sets
        SetFact<String> f1 = newSetFact("x", "y");
        SetFact<String> f2 = newSetFact("y", "z");
        SetFact<String> f3 = f1.intersectWith(f2);
        Assert.assertEquals(f3.size(), 1);

        // Intersect two disjoint sets
        f1 = newSetFact("a", "b");
        f2 = newSetFact("c", "d");
        f3 = f1.intersectWith(f2);
        Assert.assertEquals(f3.size(), 0);
        Assert.assertTrue(f3.isEmpty());

        // Intersect empty set
        f1 = newSetFact();
        f2 = newSetFact("xxx", "yyy");
        f3 = f1.intersectWith(f2);
        Assert.assertEquals(f3.size(), 0);
        Assert.assertTrue(f3.isEmpty());
    }

    @SafeVarargs
    private static <T> SetFact<T> newSetFact(T... args) {
        return new SetFact<>(Arrays.asList(args));
    }
}
