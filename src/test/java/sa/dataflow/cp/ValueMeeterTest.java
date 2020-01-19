package sa.dataflow.cp;

import org.junit.Assert;
import org.junit.Test;
import sa.dataflow.analysis.Meeter;

public class ValueMeeterTest {

    private Value i1 = Value.makeInt(1);
    private Value i0 = Value.makeInt(0);
    private Value bTrue = Value.makeBool(true);
    private Value bFalse = Value.makeBool(false);
    private Value NAC = Value.getNAC();
    private Value undef = Value.getUndef();
    private Meeter<Value> meeter = new ValueMeeter();

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
