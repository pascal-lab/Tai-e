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

package pascal.taie.analysis.dataflow.lattice;

import org.junit.Assert;
import org.junit.Test;

abstract class FlowSetTest {

    protected abstract FlowSet<String> newFlowSet(String... strings);

    @Test
    public void testUnionNormal() {
        // Union overlapped sets
        FlowSet<String> fs1 = newFlowSet("x", "y");
        FlowSet<String> fs2 = newFlowSet("y", "z");
        FlowSet<String> fs3 = fs1.union(fs2);
        Assert.assertEquals(fs3.size(), 3);

        // Union two disjoint sets
        fs1 = newFlowSet("a", "b");
        fs2 = newFlowSet("c", "d");
        fs3 = fs1.union(fs2);
        Assert.assertEquals(fs3.size(), 4);

        // Union empty set
        fs1 = newFlowSet();
        fs2 = newFlowSet("xxx", "yyy");
        fs3 = fs1.union(fs2);
        Assert.assertEquals(fs3.size(), 2);
    }

    @Test
    public void testIntersectNormal() {
        // Intersect overlapped sets
        FlowSet<String> fs1 = newFlowSet("x", "y");
        FlowSet<String> fs2 = newFlowSet("y", "z");
        FlowSet<String> fs3 = fs1.intersect(fs2);
        Assert.assertEquals(fs3.size(), 1);

        // Intersect two disjoint sets
        fs1 = newFlowSet("a", "b");
        fs2 = newFlowSet("c", "d");
        fs3 = fs1.intersect(fs2);
        Assert.assertEquals(fs3.size(), 0);
        Assert.assertTrue(fs3.isEmpty());

        // Intersect empty set
        fs1 = newFlowSet();
        fs2 = newFlowSet("xxx", "yyy");
        fs3 = fs1.intersect(fs2);
        Assert.assertEquals(fs3.size(), 0);
        Assert.assertTrue(fs3.isEmpty());
    }
}
