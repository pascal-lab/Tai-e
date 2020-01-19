package sa.dataflow.lattice;

import org.junit.Assert;
import org.junit.Test;
import sa.util.AnalysisException;

public class CPValueTest {

    @Test
    public void testInt() {
        CPValue v1 = CPValue.makeInt(10);
        Assert.assertTrue(v1.isInt());
        Assert.assertFalse(v1.isBool() || v1.isNAC() || v1.isUndef());
        Assert.assertEquals(v1.getInt(), 10);
        CPValue v2 = CPValue.makeInt(1);
        CPValue v3 = CPValue.makeInt(10);
        Assert.assertNotEquals(v1, v2);
        Assert.assertEquals(v1, v3);
    }

    @Test
    public void testBool() {
        CPValue v1 = CPValue.makeBool(true);
        Assert.assertTrue(v1.isBool());
        Assert.assertFalse(v1.isInt() || v1.isNAC() || v1.isUndef());
        Assert.assertEquals(v1.getBool(), true);
        CPValue v2 = CPValue.makeBool(false);
        CPValue v3 = CPValue.makeBool(true);
        Assert.assertNotEquals(v1, v2);
        Assert.assertEquals(v1, v3);
    }

    @Test(expected = AnalysisException.class)
    public void testGetIntOnBool() {
        CPValue.makeBool(true).getInt();
    }

    @Test(expected = AnalysisException.class)
    public void testGetIntOnNAC() {
        CPValue.getNAC().getInt();
    }

    @Test(expected = AnalysisException.class)
    public void testGetIntOnUndef() {
        CPValue.getUndef().getInt();
    }

    @Test(expected = AnalysisException.class)
    public void testGetBoolOnInt() {
        CPValue.makeInt(0).getBool();
    }

    @Test(expected = AnalysisException.class)
    public void testGetBoolOnNAC() {
        CPValue.getNAC().getBool();
    }

    @Test(expected = AnalysisException.class)
    public void testGetBoolOnUndef() {
        CPValue.getUndef().getBool();
    }
}
