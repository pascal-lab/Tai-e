/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.analysis.dfa.analysis.constprop;

import org.junit.Assert;
import org.junit.Test;
import pascal.taie.util.AnalysisException;

public class ValueTest {

    @Test
    public void testInt() {
        pascal.taie.analysis.dataflow.clients.constprop.Value v1 = pascal.taie.analysis.dataflow.clients.constprop.Value.makeConstant(10);
        Assert.assertTrue(v1.isConstant());
        Assert.assertFalse(v1.isNAC() || v1.isUndef());
        Assert.assertEquals(v1.getConstant(), 10);
        pascal.taie.analysis.dataflow.clients.constprop.Value v2 = pascal.taie.analysis.dataflow.clients.constprop.Value.makeConstant(1);
        pascal.taie.analysis.dataflow.clients.constprop.Value v3 = pascal.taie.analysis.dataflow.clients.constprop.Value.makeConstant(10);
        Assert.assertNotEquals(v1, v2);
        Assert.assertEquals(v1, v3);
    }

    @Test(expected = AnalysisException.class)
    public void testGetIntOnNAC() {
        pascal.taie.analysis.dataflow.clients.constprop.Value.getNAC().getConstant();
    }

    @Test(expected = AnalysisException.class)
    public void testGetIntOnUndef() {
        Value.getUndef().getConstant();
    }
}
