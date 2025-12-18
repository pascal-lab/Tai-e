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

package pascal.taie.util.graph;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GraphTest {

    private static final Graph<Integer> G_SIMPLE = buildGraph(
            1, 5,
            1, 3,
            3, 6,
            9, 8
    );

    @Test
    void testSimpleGraph() {
        assertEquals(6, G_SIMPLE.nodeCount());
        assertTrue(G_SIMPLE.hasNode(1));
        assertFalse(G_SIMPLE.hasNode(10));
        assertTrue(G_SIMPLE.hasEdge(3, 6));
    }

    @Test
    void testSimpleGraphCopy() {
        SimpleGraph<Integer> copy = new SimpleGraph<>(G_SIMPLE);
        assertEquals(G_SIMPLE.nodeCount(), copy.nodeCount());
        for (Integer node : G_SIMPLE) {
            for (Integer succ : G_SIMPLE.getSuccsOf(node)) {
                assertTrue(copy.hasEdge(node, succ));
            }
        }
    }

    @Test
    void testSimpleGraphRemove() {
        SimpleGraph<Integer> g = new SimpleGraph<>(G_SIMPLE);
        assertEquals(6, g.nodeCount());
        assertTrue(g.hasEdge(1, 3));
        assertTrue(g.hasEdge(1, 5));

        g.removeNode(1);
        assertEquals(5, g.nodeCount());
        assertFalse(g.hasEdge(1, 3));
        assertFalse(g.hasEdge(1, 5));

        assertTrue(g.hasEdge(3, 6));
        g.removeEdge(3, 6);
        assertFalse(g.hasEdge(3, 6));
    }

    private static final Graph<Integer> G_TOPSORT = buildGraph(
            1, 2,
            2, 3,
            3, 4,
            5, 3,
            6, 5,
            7, 8,
            9, 10
    );

    @Test
    void testTopSort() {
        List<Integer> l = new TopologicalSorter<>(G_TOPSORT).get();
        assertTrue(l.indexOf(1) < l.indexOf(4));
        assertTrue(l.indexOf(5) < l.indexOf(3));
        assertTrue(l.indexOf(5) < l.indexOf(4));
        assertTrue(l.indexOf(6) < l.indexOf(4));

        List<Integer> rl = new TopologicalSorter<>(G_TOPSORT, true).get();
        assertTrue(rl.indexOf(1) > rl.indexOf(4));
        assertTrue(rl.indexOf(5) > rl.indexOf(3));
        assertTrue(rl.indexOf(5) > rl.indexOf(4));
        assertTrue(rl.indexOf(6) > rl.indexOf(4));
    }

    private static final Graph<Integer> G_SCC = buildGraph(
            1, 1,
            2, 4,
            4, 6,
            8, 9,
            9, 8,
            10, 11,
            11, 12,
            12, 13,
            13, 11
    );

    @Test
    void testSCC() {
        SCC<Integer> scc = new SCC<>(G_SCC);
        assertEquals(7, scc.getComponents().size());
        assertEquals(3, scc.getTrueComponents().size());
    }

    @Test
    void testMergedSCC() {
        MergedSCCGraph<Integer> mg = new MergedSCCGraph<>(G_SCC);
        assertEquals(7, mg.nodeCount());
    }

    private static final Graph<Integer> G_DOM = buildGraph(
            1, 2,
            1, 3,
            2, 3,
            3, 4,
            4, 3,
            4, 5,
            4, 6,
            5, 7,
            6, 7,
            7, 4,
            7, 8,
            8, 3,
            8, 9,
            8, 10,
            10, 7
    );

    @Test
    void testDominator() {
        DominatorFinder<Integer> domFinder = new DominatorFinder<>(G_DOM);
        assertTrue(domFinder.isDominatedBy(2, 1));
        assertFalse(domFinder.isDominatedBy(1, 2));

        assertEquals(domFinder.getDominatorsOf(1), Set.of(1));
        assertEquals(domFinder.getDominatorsOf(3), Set.of(1, 3));
        assertEquals(domFinder.getDominatorsOf(5), Set.of(1, 3, 4, 5));
        assertEquals(domFinder.getDominatorsOf(7), Set.of(1, 3, 4, 7));
        assertEquals(domFinder.getDominatorsOf(9), Set.of(1, 3, 4, 7, 8, 9));

        assertEquals(domFinder.getNodesDominatedBy(1),
                Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        assertEquals(domFinder.getNodesDominatedBy(3),
                Set.of(3, 4, 5, 6, 7, 8, 9, 10));
        assertEquals(domFinder.getNodesDominatedBy(5), Set.of(5));
        assertEquals(domFinder.getNodesDominatedBy(7), Set.of(7, 8, 9, 10));
        assertEquals(domFinder.getNodesDominatedBy(9), Set.of(9));
    }

    private static SimpleGraph<Integer> buildGraph(int... nodes) {
        assert nodes.length % 2 == 0;
        SimpleGraph<Integer> graph = new SimpleGraph<>();
        for (int i = 0; i < nodes.length; i += 2) {
            graph.addEdge(nodes[i], nodes[i + 1]);
        }
        return graph;
    }
}
