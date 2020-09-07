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

package pascal.panda;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import pascal.panda.callgraph.cha.CHATestFull;
import pascal.panda.dataflow.DataFlowTestSuite;
import pascal.panda.dataflow.analysis.constprop.CPTestSuite;
import pascal.panda.dataflow.lattice.LatticeTestSuite;
import pascal.panda.options.OptionsTest;
import pascal.panda.pta.CSPTATest;
import pascal.panda.pta.PTATestFull;
import pascal.panda.util.ArraySetTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        CPTestSuite.class,
        LatticeTestSuite.class,
        DataFlowTestSuite.class,
        CHATestFull.class,
        PTATestFull.class,
        CSPTATest.class,
        OptionsTest.class,
        ArraySetTest.class,
})
public class PandaTestSuite {
}
