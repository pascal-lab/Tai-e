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

package pascal.taie.analysis.dfa;

import org.junit.Assert;
import org.junit.Test;
import pascal.taie.analysis.dfa.fact.ToppedSetFact;

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
}
