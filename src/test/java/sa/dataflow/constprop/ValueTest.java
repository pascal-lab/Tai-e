package sa.dataflow.constprop;

import org.junit.Assert;
import org.junit.Test;
import sa.util.AnalysisException;

public class ValueTest {

    @Test
    public void testInt() {
        Value v1 = Value.makeInt(10);
        Assert.assertTrue(v1.isInt());
        Assert.assertFalse(v1.isBool() || v1.isNAC() || v1.isUndef());
        Assert.assertEquals(v1.getInt(), 10);
        Value v2 = Value.makeInt(1);
        Value v3 = Value.makeInt(10);
        Assert.assertNotEquals(v1, v2);
        Assert.assertEquals(v1, v3);
    }

    @Test
    public void testBool() {
        Value v1 = Value.makeBool(true);
        Assert.assertTrue(v1.isBool());
        Assert.assertFalse(v1.isInt() || v1.isNAC() || v1.isUndef());
        Assert.assertEquals(v1.getBool(), true);
        Value v2 = Value.makeBool(false);
        Value v3 = Value.makeBool(true);
        Assert.assertNotEquals(v1, v2);
        Assert.assertEquals(v1, v3);
    }

    @Test(expected = AnalysisException.class)
    public void testGetIntOnBool() {
        Value.makeBool(true).getInt();
    }

    @Test(expected = AnalysisException.class)
    public void testGetIntOnNAC() {
        Value.getNAC().getInt();
    }

    @Test(expected = AnalysisException.class)
    public void testGetIntOnUndef() {
        Value.getUndef().getInt();
    }

    @Test(expected = AnalysisException.class)
    public void testGetBoolOnInt() {
        Value.makeInt(0).getBool();
    }

    @Test(expected = AnalysisException.class)
    public void testGetBoolOnNAC() {
        Value.getNAC().getBool();
    }

    @Test(expected = AnalysisException.class)
    public void testGetBoolOnUndef() {
        Value.getUndef().getBool();
    }
}
