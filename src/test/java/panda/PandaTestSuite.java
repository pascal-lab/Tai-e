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

package panda;

import panda.callgraph.cha.CHATestFull;
import panda.dataflow.DataFlowTestSuite;
import panda.dataflow.analysis.constprop.CPTestSuite;
import panda.dataflow.lattice.LatticeTestSuite;
import panda.options.OptionsTest;
import panda.pta.CSPTATest;
import panda.pta.PTATestFull;
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
public class PandaTestSuite {
}
