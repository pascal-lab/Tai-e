package pascal.taie.frontend.newfrontend;

import org.junit.Assert;
import org.junit.Test;
import pascal.taie.frontend.newfrontend.ssa.Dominator;
import pascal.taie.frontend.newfrontend.ssa.IndexedGraph;

import java.util.ArrayList;
import java.util.List;

public class TestDom {
    @Test
    public void simple0() {
        // unit graph
        List<List<Integer>> g = List.of(List.of());
        int[] idom = runDomTest(g);
        Assert.assertArrayEquals(new int[]{0}, idom);
        Dominator.DominatorFrontiers df = new Dominator(loadTest(g)).getDF();
        Assert.assertEquals(0, df.get(0).size());
    }

    @Test
    public void simple1() {
        // a cfg with branch
        // 0 -> 1 -> 3
        // 0 -> 2 -> 3
        List<List<Integer>> g = List.of(
                List.of(1, 2),
                List.of(3),
                List.of(3),
                List.of()
        );
        int[] idom = runDomTest(g);
        Assert.assertArrayEquals(new int[]{0, 0, 0, 0}, idom);
        Dominator.DominatorFrontiers df = new Dominator(loadTest(g)).getDF();
        Assert.assertEquals(0, df.get(0).size());
        Assert.assertEquals(List.of(3), df.get(1).toList());
        Assert.assertEquals(List.of(3), df.get(2).toList());
        Assert.assertEquals(0, df.get(3).size());
    }

    @Test
    public void std1() {
        // from ssa book, pp. 30, Fig 3.2
        List<List<Integer>> g = List.of(
                List.of(1),
                List.of(2, 3),
                List.of(4),
                List.of(4, 5),
                List.of(5, 1),
                List.of()
        );
        int[] idom = runDomTest(g);
        Assert.assertArrayEquals(new int[]{0, 0, 1, 1, 1, 1}, idom);
        Dominator.DominatorFrontiers df = new Dominator(loadTest(g)).getDF();
        Assert.assertEquals(0, df.get(0).size());
        Assert.assertEquals(List.of(1), df.get(1).toList());
        Assert.assertEquals(List.of(4), df.get(2).toList());
        Assert.assertEquals(List.of(4, 5), df.get(3).toList());
        Assert.assertEquals(List.of(1, 5), df.get(4).toList());
        Assert.assertEquals(List.of(), df.get(5).toList());
    }

    @Test
    public void std2() {
        // from ssa book, pp. 47, Fig 4.1
        List<List<Integer>> g = List.of(
                List.of(1),
                List.of(2, 10),
                List.of(3, 7),
                List.of(4),
                List.of(5),
                List.of(6, 4),
                List.of(1),
                List.of(8),
                List.of(5, 9),
                List.of(7),
                List.of()
        );
        int[] idom = runDomTest(g);
        Assert.assertArrayEquals(new int[]{0, 0, 1, 2, 2, 2, 5, 2, 7, 8, 1}, idom);
        Dominator.DominatorFrontiers df = new Dominator(loadTest(g)).getDF();
        Assert.assertEquals(0, df.get(0).size());
        Assert.assertEquals(List.of(1), df.get(1).toList());
        Assert.assertEquals(List.of(1), df.get(2).toList());
        Assert.assertEquals(List.of(4), df.get(3).toList());
        Assert.assertEquals(List.of(5), df.get(4).toList());
        Assert.assertEquals(List.of(1, 4), df.get(5).toList());
        Assert.assertEquals(List.of(1), df.get(6).toList());
        Assert.assertEquals(List.of(5, 7), df.get(7).toList());
        Assert.assertEquals(List.of(5, 7), df.get(8).toList());
        Assert.assertEquals(List.of(7), df.get(9).toList());
        Assert.assertEquals(List.of(), df.get(10).toList());
    }

    @Test
    public void std3() {
        // from "A Simple, Fast Dominance Algorithm", pp. 14, Fig. 4
        List<List<Integer>> g = List.of(
                List.of(1, 2),
                List.of(3),
                List.of(4, 5),
                List.of(4),
                List.of(3, 5),
                List.of(4)
        );
        int[] idom = runDomTest(g);
        Assert.assertArrayEquals(new int[]{0, 0, 0, 0, 0, 0}, idom);
        Dominator.DominatorFrontiers df = new Dominator(loadTest(g)).getDF();
        Assert.assertEquals(0, df.get(0).size());
        Assert.assertEquals(List.of(3), df.get(1).toList());
        Assert.assertEquals(List.of(4, 5), df.get(2).toList());
        Assert.assertEquals(List.of(4), df.get(3).toList());
        Assert.assertEquals(List.of(3, 5), df.get(4).toList());
        Assert.assertEquals(List.of(4), df.get(5).toList());

    }

    @Test
    public void std4() {
        // from https://github.com/julianjensen/dominators/blob/master/data/dj.txt
        // commit: 8a9e07a
        // graph:
        // 0 > 1
        // 1 > 2 10
        // 2 > 3 7
        // 3 > 4
        // 4 > 5
        // 5 > 4 6
        // 6 > 1
        // 7 > 8
        // 8 > 5 9
        // 9 > 7
        // 10 >
        List<List<Integer>> g = List.of(
                List.of(1),
                List.of(2, 10),
                List.of(3, 7),
                List.of(4),
                List.of(5),
                List.of(4, 6),
                List.of(1),
                List.of(8),
                List.of(5, 9),
                List.of(7),
                List.of()
        );
        int[] idom = runDomTest(g);
        Assert.assertArrayEquals(new int[]{0, 0, 1, 2, 2, 2, 5, 2, 7, 8, 1}, idom);
    }

    @Test
    public void std5() {
        // from https://www.cs.cmu.edu/afs/cs/academic/class/15745-s06/web/handouts/ssa15745.pdf
        // pp. 11
        List<List<Integer>> g = List.of(
                List.of(1, 4),
                List.of(2, 3),
                List.of(3),
                List.of(),
                List.of(5),
                List.of(3, 4)
        );
        int[] idom = runDomTest(g);
        Assert.assertArrayEquals(new int[]{0, 0, 1, 0, 0, 4}, idom);
    }

    @Test
    public void std6() {
        // from https://www.cs.cmu.edu/afs/cs/academic/class/15745-s06/web/handouts/ssa15745.pdf
        // pp. 12
        List<List<Integer>> g = List.of(
                List.of(1, 4, 8),
                List.of(2),
                List.of(2, 3),
                List.of(12),
                List.of(5, 6),
                List.of(3, 7),
                List.of(7, 11),
                List.of(4, 12),
                List.of(9, 10),
                List.of(11),
                List.of(11),
                List.of(12),
                List.of()
        );
        int[] idom = runDomTest(g);
        Assert.assertArrayEquals(new int[]{0, 0, 1, 0, 0, 4, 4, 4, 0, 8, 8, 0, 0}, idom);
    }

    static int[] runDomTest(List<List<Integer>> g) {
        return new Dominator(loadTest(g)).getDomTree();
    }

    static IndexedGraph<Integer> loadTest(List<List<Integer>> directedGraph) {
        List<List<Integer>> inEdges = new ArrayList<>();
        for (List<Integer> ignored : directedGraph) {
            inEdges.add(new ArrayList<>());
        }
        for (int i = 0; i < directedGraph.size(); ++i) {
            for (int j : directedGraph.get(i)) {
                inEdges.get(j).add(i);
            }
        }
        List<Integer> entries = new ArrayList<>();
        for (int i = 0; i < inEdges.size(); ++i) {
            if (inEdges.get(i).isEmpty()) {
                entries.add(i);
            }
        }
        return new IndexedGraph<>() {
            @Override
            public List<Integer> inEdges(int index) {
                return inEdges.get(index);
            }

            @Override
            public List<Integer> outEdges(int index) {
                return directedGraph.get(index);
            }

            @Override
            public Integer getNode(int index) {
                return index;
            }

            @Override
            public int getIndex(Integer node) {
                return node;
            }

            @Override
            public int size() {
                return directedGraph.size();
            }

            @Override
            public Integer getEntry() {
                return entries.get(0);
            }
        };
    }
}
