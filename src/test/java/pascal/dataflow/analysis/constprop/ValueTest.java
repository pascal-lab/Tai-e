package pascal.dataflow.analysis.constprop;

import org.junit.Assert;
import org.junit.Test;
import pascal.util.AnalysisException;

public class ValueTest {

    @Test
    public void testInt() {
        Value v1 = Value.makeConstant(10);
        Assert.assertTrue(v1.isConstant());
        Assert.assertFalse(v1.isNAC() || v1.isUndef());
        Assert.assertEquals(v1.getConstant(), 10);
        Value v2 = Value.makeConstant(1);
        Value v3 = Value.makeConstant(10);
        Assert.assertNotEquals(v1, v2);
        Assert.assertEquals(v1, v3);
    }

    @Test(expected = AnalysisException.class)
    public void testGetIntOnNAC() {
        Value.getNAC().getConstant();
    }

    @Test(expected = AnalysisException.class)
    public void testGetIntOnUndef() {
        Value.getUndef().getConstant();
    }
}
