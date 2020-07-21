/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C)  2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C)  2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo;

import bamboo.callgraph.cha.CHATestFull;
import bamboo.dataflow.DataFlowTestSuite;
import bamboo.dataflow.analysis.constprop.CPTestSuite;
import bamboo.dataflow.lattice.LatticeTestSuite;
import bamboo.options.OptionsTest;
import bamboo.pta.CSPTATest;
import bamboo.pta.PTATestFull;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        CPTestSuite.class,
        LatticeTestSuite.class,
        DataFlowTestSuite.class,
        CHATestFull.class,
        PTATestFull.class,
        CSPTATest.class,
        OptionsTest.class,
})
public class BambooTestSuite {
}
