package sa.dataflow.lattice;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

abstract class FlowSetTest {

    protected FlowSetFactory<String> factory;

    @Test
    public void testAdd() {
        FlowSet<String> bottom = factory.newFlowSet();
        Assert.assertTrue(bottom.isEmpty());

        FlowSet<String> fs1 = bottom.add("x");
        Assert.assertFalse(fs1.isEmpty());

        FlowSet<String> fs2 = fs1.add("y");
        Assert.assertEquals(fs2.getElements().size(), 2);
    }

    @Test
    public void testRemove() {
        FlowSet<String> bottom = factory.newFlowSet();
        Assert.assertTrue(bottom.isEmpty());

        FlowSet<String> fs1 = bottom.remove("x");
        Assert.assertTrue(fs1.isEmpty());

        FlowSet<String> fs2 =
                factory.newFlowSet(Collections.singleton("x"));
        FlowSet<String> fs3 = fs2.remove("y");
        Assert.assertFalse(fs3.isEmpty());
        FlowSet<String> fs4 = fs2.remove("x");
        Assert.assertTrue(fs4.isEmpty());
    }

    @Test
    public void testUnionNormal() {
        // Union overlapped sets
        FlowSet<String> fs1 = factory.newFlowSet("x", "y");
        FlowSet<String> fs2 = factory.newFlowSet("y", "z");
        FlowSet<String> fs3 = fs1.union(fs2);
        Assert.assertEquals(fs3.size(), 3);

        // Union two disjoint sets
        fs1 = factory.newFlowSet("a", "b");
        fs2 = factory.newFlowSet("c", "d");
        fs3 = fs1.union(fs2);
        Assert.assertEquals(fs3.size(), 4);

        // Union empty set
        fs1 = factory.newFlowSet();
        fs2 = factory.newFlowSet("xxx", "yyy");
        fs3 = fs1.union(fs2);
        Assert.assertEquals(fs3.size(), 2);
    }

    @Test
    public void testIntersectNormal() {
        // Intersect overlapped sets
        FlowSet<String> fs1 = factory.newFlowSet("x", "y");
        FlowSet<String> fs2 = factory.newFlowSet("y", "z");
        FlowSet<String> fs3 = fs1.intersect(fs2);
        Assert.assertEquals(fs3.size(), 1);

        // Intersect two disjoint sets
        fs1 = factory.newFlowSet("a", "b");
        fs2 = factory.newFlowSet("c", "d");
        fs3 = fs1.intersect(fs2);
        Assert.assertEquals(fs3.size(), 0);
        Assert.assertTrue(fs3.isEmpty());

        // Intersect empty set
        fs1 = factory.newFlowSet();
        fs2 = factory.newFlowSet("xxx", "yyy");
        fs3 = fs1.intersect(fs2);
        Assert.assertEquals(fs3.size(), 0);
        Assert.assertTrue(fs3.isEmpty());
    }
}
