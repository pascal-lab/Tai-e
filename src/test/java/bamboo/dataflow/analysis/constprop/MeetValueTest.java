package bamboo.dataflow.analysis.constprop;

import org.junit.Assert;
import org.junit.Test;

public class MeetValueTest {

    private Value i1 = Value.makeConstant(1);
    private Value i0 = Value.makeConstant(0);
    private Value NAC = Value.getNAC();
    private Value undef = Value.getUndef();
    private ConstantPropagation cp = ConstantPropagation.v();

    @Test
    public void testMeet() {
        Assert.assertEquals(cp.meetValue(undef, undef), undef);
        Assert.assertEquals(cp.meetValue(undef, i0), i0);
        Assert.assertEquals(cp.meetValue(undef, NAC), NAC);
        Assert.assertEquals(cp.meetValue(NAC, NAC), NAC);
        Assert.assertEquals(cp.meetValue(NAC, i0), NAC);
        Assert.assertEquals(cp.meetValue(NAC, undef), NAC);
        Assert.assertEquals(cp.meetValue(i0, i0), i0);
        Assert.assertEquals(cp.meetValue(i0, i1), NAC);
        Assert.assertEquals(cp.meetValue(i0, undef), i0);
        Assert.assertEquals(cp.meetValue(i0, NAC), NAC);
    }
}
