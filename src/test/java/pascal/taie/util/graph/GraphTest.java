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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

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

    private static Graph<Integer> readGraph(String filePath) {
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
