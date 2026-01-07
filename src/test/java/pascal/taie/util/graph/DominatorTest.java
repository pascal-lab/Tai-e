package pascal.taie.util.graph;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DominatorTest {

    private static final Integer ENTRY = 0;

    @Test
    public void testSingleNodeGraph() {
        int[][] succs = {{}};
        Dominators<Integer> dom = buildDominators(succs);
        assertArrayEquals(new int[]{0}, dom.getIDom());
        assertEquals(0, dom.getDomFrontier().get(0).size());
    }

    @Test
    public void testBranch() {
        // a cfg with branch
        // 0 -> 1 -> 3
        // 0 -> 2 -> 3
        int[][] succs = {
                {1, 2},
                {3},
                {3},
                {}
        };
        Dominators<Integer> dom = buildDominators(succs);
        assertArrayEquals(new int[]{0, 0, 0, 0}, dom.getIDom());
        Dominators.DominatorFrontiers df = dom.getDomFrontier();
        assertEquals(0, df.get(0).size());
        assertArrayEquals(new int[]{3}, df.get(1).toArray());
        assertArrayEquals(new int[]{3}, df.get(2).toArray());
        assertEquals(0, df.get(3).size());
    }

    @Test
    public void testSSABookCase1() {
        // from ssa book, pp. 30, Fig 3.2
        int[][] succs = {
                {1},
                {2, 3},
                {4},
                {4, 5},
                {5, 1},
                {}
        };
        Dominators<Integer> dom = buildDominators(succs);
        assertArrayEquals(new int[]{0, 0, 1, 1, 1, 1}, dom.getIDom());
        Dominators.DominatorFrontiers df = dom.getDomFrontier();
        assertEquals(0, df.get(0).size());
        assertArrayEquals(new int[]{1}, df.get(1).toArray());
        assertArrayEquals(new int[]{4}, df.get(2).toArray());
        assertArrayEquals(new int[]{4, 5}, df.get(3).toArray());
        assertArrayEquals(new int[]{1, 5}, df.get(4).toArray());
        assertArrayEquals(new int[]{}, df.get(5).toArray());
    }

    @Test
    public void testSSABookCase2() {
        // from ssa book, pp. 47, Fig 4.1
        int[][] succs = {
                {1},
                {2, 10},
                {3, 7},
                {4},
                {5},
                {6, 4},
                {1},
                {8},
                {5, 9},
                {7},
                {}
        };
        Dominators<Integer> dom = buildDominators(succs);
        assertArrayEquals(new int[]{0, 0, 1, 2, 2, 2, 5, 2, 7, 8, 1}, dom.getIDom());
        Dominators.DominatorFrontiers df = dom.getDomFrontier();
        assertEquals(0, df.get(0).size());
        assertArrayEquals(new int[]{1}, df.get(1).toArray());
        assertArrayEquals(new int[]{1}, df.get(2).toArray());
        assertArrayEquals(new int[]{4}, df.get(3).toArray());
        assertArrayEquals(new int[]{5}, df.get(4).toArray());
        assertArrayEquals(new int[]{1, 4}, df.get(5).toArray());
        assertArrayEquals(new int[]{1}, df.get(6).toArray());
        assertArrayEquals(new int[]{5, 7}, df.get(7).toArray());
        assertArrayEquals(new int[]{5, 7}, df.get(8).toArray());
        assertArrayEquals(new int[]{7}, df.get(9).toArray());
        assertArrayEquals(new int[]{}, df.get(10).toArray());
    }

    @Test
    public void testOriginPaperCase() {
        // from "A Simple, Fast Dominance Algorithm", pp. 14, Fig. 4
        int[][] succs = {
                {1, 2},
                {3},
                {4, 5},
                {4},
                {3, 5},
                {4}
        };
        Dominators<Integer> dom = buildDominators(succs);
        assertArrayEquals(new int[]{0, 0, 0, 0, 0, 0}, dom.getIDom());
        Dominators.DominatorFrontiers df = dom.getDomFrontier();
        assertEquals(0, df.get(0).size());
        assertArrayEquals(new int[]{3}, df.get(1).toArray());
        assertArrayEquals(new int[]{4, 5}, df.get(2).toArray());
        assertArrayEquals(new int[]{4}, df.get(3).toArray());
        assertArrayEquals(new int[]{3, 5}, df.get(4).toArray());
        assertArrayEquals(new int[]{4}, df.get(5).toArray());
    }

    @Test
    public void testJulian() {
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
        int[][] succs = {
                {1},
                {2, 10},
                {3, 7},
                {4},
                {5},
                {4, 6},
                {1},
                {8},
                {5, 9},
                {7},
                {}
        };
        assertArrayEquals(new int[]{0, 0, 1, 2, 2, 2, 5, 2, 7, 8, 1},
                buildDominators(succs).getIDom());
    }

    @Test
    public void testCMUCourseCase1() {
        // from https://www.cs.cmu.edu/afs/cs/academic/class/15745-s06/web/handouts/ssa15745.pdf
        // pp. 11
        int[][] succs = {
                {1, 4},
                {2, 3},
                {3},
                {},
                {5},
                {3, 4}
        };
        assertArrayEquals(new int[]{0, 0, 1, 0, 0, 4},
                buildDominators(succs).getIDom());
    }

    @Test
    public void testCMUCourseCase2() {
        // from https://www.cs.cmu.edu/afs/cs/academic/class/15745-s06/web/handouts/ssa15745.pdf
        // pp. 12
        int[][] succs = {
                {1, 4, 8},
                {2},
                {2, 3},
                {12},
                {5, 6},
                {3, 7},
                {7, 11},
                {4, 12},
                {9, 10},
                {11},
                {11},
                {12},
                {}
        };
        assertArrayEquals(new int[]{0, 0, 1, 0, 0, 4, 4, 4, 0, 8, 8, 0, 0},
                buildDominators(succs).getIDom());
    }

    @Test
    public void testDragonBookCase() {
        // from the dragon book (2nd Eds.), pp. 657
        int[][] succs = {
                {1, 2},
                {2},
                {3},
                {2, 4, 5},
                {6},
                {6},
                {3, 7},
                {2, 8, 9},
                {0},
                {6}
        };
        assertArrayEquals(new int[]{0, 0, 0, 2, 3, 3, 3, 6, 7, 7},
                buildDominators(succs).getIDom());
    }

    private static Dominators<Integer> buildDominators(int[][] succs) {
        SimpleIndexedGraph graph = new SimpleIndexedGraph();
        for (int node = 0; node < succs.length; node++) {
            graph.addNode(node);
            for (int i = 0; i < succs[node].length; i++) {
                graph.addEdge(node, succs[node][i]);
            }
        }
        return new Dominators<>(graph);
    }

    private static class SimpleIndexedGraph
            extends SimpleGraph<Integer>
            implements IndexedGraph<Integer> {

        @Override
        public Integer getEntry() {
            return ENTRY;
        }

        @Override
        public int getIndex(Integer o) {
            return o;
        }

        @Override
        public Integer getObject(int index) {
            return index;
        }
    }
}
