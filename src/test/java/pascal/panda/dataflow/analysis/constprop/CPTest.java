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

import org.junit.Test;
import pascal.panda.TestUtils;

public class CPTest {

    @Test
    public void testSimpleConstant() {
        TestUtils.testCP("SimpleConstant");
    }

    @Test
    public void testSimpleBinary() {
        TestUtils.testCP("SimpleBinary");
    }

    @Test
    public void testSimpleBranch() {
        TestUtils.testCP("SimpleBranch");
    }
}
