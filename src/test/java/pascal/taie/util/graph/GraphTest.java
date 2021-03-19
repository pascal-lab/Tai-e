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

package pascal.taie.util.graph;

import org.junit.Assert;
import org.junit.Test;

public class GraphTest {

    @Test
    public void testSimpleGraph() {
        SimpleGraph<Integer> g = new SimpleGraph<>();
        g.addEdge(1, 5);
        g.addEdge(1, 3);
        g.addEdge(3, 6);
        g.addEdge(9, 8);
        Assert.assertEquals(g.getNumberOfNodes(), 6);
        Assert.assertTrue(g.hasNode(1));
        Assert.assertFalse(g.hasNode(10));
        Assert.assertTrue(g.hasEdge(3, 6));
    }
}
