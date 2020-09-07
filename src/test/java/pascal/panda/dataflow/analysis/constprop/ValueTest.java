/*
 * Panda - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Panda is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.panda.dataflow.analysis.constprop;

import pascal.panda.util.AnalysisException;
import org.junit.Assert;
import org.junit.Test;

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
