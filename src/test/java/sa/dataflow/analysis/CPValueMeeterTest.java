package sa.dataflow.analysis;

import org.junit.Assert;
import org.junit.Test;
import sa.dataflow.lattice.CPValue;

public class CPValueMeeterTest {

    private CPValue i1 = CPValue.makeInt(1);
    private CPValue i0 = CPValue.makeInt(0);
    private CPValue bTrue = CPValue.makeBool(true);
    private CPValue bFalse = CPValue.makeBool(false);
    private CPValue NAC = CPValue.getNAC();
    private CPValue undef = CPValue.getUndef();
    private Meeter<CPValue> meeter = new CPValueMeeter();

    @Test
    public void testMeet() {
        Assert.assertEquals(meeter.meet(undef, undef), undef);
        Assert.assertEquals(meeter.meet(undef, i0), i0);
        Assert.assertEquals(meeter.meet(undef, bTrue), bTrue);
        Assert.assertEquals(meeter.meet(undef, NAC), NAC);
        Assert.assertEquals(meeter.meet(NAC, NAC), NAC);
        Assert.assertEquals(meeter.meet(NAC, i0), NAC);
        Assert.assertEquals(meeter.meet(NAC, bTrue), NAC);
        Assert.assertEquals(meeter.meet(NAC, undef), NAC);
        Assert.assertEquals(meeter.meet(i0, i0), i0);
        Assert.assertEquals(meeter.meet(i0, i1), NAC);
        Assert.assertEquals(meeter.meet(i0, undef), i0);
        Assert.assertEquals(meeter.meet(i0, NAC), NAC);
        Assert.assertEquals(meeter.meet(bTrue, bTrue), bTrue);
        Assert.assertEquals(meeter.meet(bTrue, bFalse), NAC);
        Assert.assertEquals(meeter.meet(bTrue, undef), bTrue);
        Assert.assertEquals(meeter.meet(bTrue, NAC), NAC);
    }
}
