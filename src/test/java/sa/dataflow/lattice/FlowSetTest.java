package sa.dataflow.lattice;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

abstract class FlowSetTest {

    protected FlowSetFactory<String> factory;

    @Test
    public void testAdd() {
        FlowSet<String> bottom = factory.getBottom();
        Assert.assertTrue(bottom.isBottom());

        FlowSet<String> fs1 = bottom.add("x");
        Assert.assertFalse(fs1.isBottom());

        FlowSet<String> fs2 = fs1.add("y");
        Assert.assertEquals(fs2.getElements().size(), 2);

        FlowSet<String> top = factory.getTop();
        FlowSet<String> fs3 = top.add("y");
        Assert.assertTrue(fs3.isTop());
    }

    @Test
    public void testRemove() {
        FlowSet<String> bottom = factory.getBottom();
        Assert.assertTrue(bottom.isBottom());

        FlowSet<String> fs1 = bottom.remove("x");
        Assert.assertTrue(fs1.isBottom());

        FlowSet<String> fs2 =
                factory.newFlowSet(Collections.singleton("x"));
        FlowSet<String> fs3 = fs2.remove("y");
        Assert.assertFalse(fs3.isBottom());
        FlowSet<String> fs4 = fs2.remove("x");
        Assert.assertTrue(fs4.isBottom());
    }

    @Test
    public void testUnionNormal() {

    }
}
