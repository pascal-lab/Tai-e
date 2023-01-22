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

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class GraphTest {

    private static Graph<Integer> genRandomGraph(int n) {
        SimpleGraph<Integer> graph = new SimpleGraph<>();
        Random random = new Random(System.currentTimeMillis());
        for (int i = 0; i < n; ++i) {
            graph.addNode(i);
        }
        for (int i = 0; i < 2 * n; ++i) {
            graph.addEdge(random.nextInt(n), random.nextInt(n));
        }
        return graph;
    }

    @Test
    public void testSimpleGraph() {
        Graph<Integer> g = readGraph("src/test/resources/util/graph-simple.txt");
        Assert.assertEquals(6, g.getNumberOfNodes());
        Assert.assertTrue(g.hasNode(1));
        Assert.assertFalse(g.hasNode(10));
        Assert.assertTrue(g.hasEdge(3, 6));
    }

    @Test
    public void testSimpleGraphCopy() {
        Graph<Integer> g = readGraph("src/test/resources/util/graph-simple.txt");
        SimpleGraph<Integer> copy = new SimpleGraph<>(g);
        Assert.assertEquals(g.getNumberOfNodes(), copy.getNumberOfNodes());
        for (Integer node : g) {
            for (Integer succ : g.getSuccsOf(node)) {
                Assert.assertTrue(copy.hasEdge(node, succ));
            }
        }
    }

    @Test
    public void testSimpleGraphRemove() {
        SimpleGraph<Integer> g = readGraph("src/test/resources/util/graph-simple.txt");
        Assert.assertEquals(6, g.getNumberOfNodes());
        Assert.assertTrue(g.hasEdge(1, 3));
        Assert.assertTrue(g.hasEdge(1, 5));

        g.removeNode(1);
        Assert.assertEquals(5, g.getNumberOfNodes());
        Assert.assertFalse(g.hasEdge(1, 3));
        Assert.assertFalse(g.hasEdge(1, 5));

        Assert.assertTrue(g.hasEdge(3, 6));
        g.removeEdge(3, 6);
        Assert.assertFalse(g.hasEdge(3, 6));
    }

    @Test
    public void testTopsort() {
        Graph<Integer> g = readGraph("src/test/resources/util/graph-topsort.txt");
        List<Integer> l = new TopoSorter<>(g).get();
        Assert.assertTrue(l.indexOf(1) < l.indexOf(4));
        Assert.assertTrue(l.indexOf(5) < l.indexOf(3));
        Assert.assertTrue(l.indexOf(5) < l.indexOf(4));
        Assert.assertTrue(l.indexOf(6) < l.indexOf(4));

        List<Integer> rl = new TopoSorter<>(g, true).get();
        Assert.assertTrue(rl.indexOf(1) > rl.indexOf(4));
        Assert.assertTrue(rl.indexOf(5) > rl.indexOf(3));
        Assert.assertTrue(rl.indexOf(5) > rl.indexOf(4));
        Assert.assertTrue(rl.indexOf(6) > rl.indexOf(4));
    }

    @Test
    public void testSCC() {
        Graph<Integer> g = readGraph("src/test/resources/util/graph-scc.txt");
        SCC<Integer> scc = new SCC<>(g);
        Assert.assertEquals(7, scc.getComponents().size());
        Assert.assertEquals(3, scc.getTrueComponents().size());
    }

    @Test
    public void testMergedSCC() {
        Graph<Integer> g = readGraph("src/test/resources/util/graph-scc.txt");
        MergedSCCGraph<Integer> mg = new MergedSCCGraph<>(g);
        Assert.assertEquals(7, mg.getNumberOfNodes());
    }

    @Test
    public void testDominator() {
        Graph<Integer> g = readGraph("src/test/resources/util/graph-dominator.txt");
        DominatorFinder<Integer> domFinder = new DominatorFinder<>(g);
        Assert.assertTrue(domFinder.isDominatedBy(2, 1));
        Assert.assertFalse(domFinder.isDominatedBy(1, 2));

        Assert.assertEquals(domFinder.getDominatorsOf(1), Set.of(1));
        Assert.assertEquals(domFinder.getDominatorsOf(3), Set.of(1, 3));
        Assert.assertEquals(domFinder.getDominatorsOf(5), Set.of(1, 3, 4, 5));
        Assert.assertEquals(domFinder.getDominatorsOf(7), Set.of(1, 3, 4, 7));
        Assert.assertEquals(domFinder.getDominatorsOf(9), Set.of(1, 3, 4, 7, 8, 9));

        Assert.assertEquals(domFinder.getNodesDominatedBy(1),
                Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        Assert.assertEquals(domFinder.getNodesDominatedBy(3),
                Set.of(3, 4, 5, 6, 7, 8, 9, 10));
        Assert.assertEquals(domFinder.getNodesDominatedBy(5), Set.of(5));
        Assert.assertEquals(domFinder.getNodesDominatedBy(7), Set.of(7, 8, 9, 10));
        Assert.assertEquals(domFinder.getNodesDominatedBy(9), Set.of(9));
    }

    private static SimpleGraph<Integer> readGraph(String filePath) {
        SimpleGraph<Integer> graph = new SimpleGraph<>();
        try {
            Files.readAllLines(Path.of(filePath)).forEach(line -> {
                String[] split = line.split("->");
                int source = Integer.parseInt(split[0]);
                int target = Integer.parseInt(split[1]);
                graph.addEdge(source, target);
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + filePath +
                    " due to " + e);
        }
        return graph;
    }
}
