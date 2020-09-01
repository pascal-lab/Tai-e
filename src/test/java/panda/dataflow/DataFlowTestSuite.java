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

package panda.dataflow;

import panda.dataflow.analysis.constprop.CPTestSuite;
import panda.dataflow.analysis.deadcode.DCDTestFull;
import panda.dataflow.lattice.LatticeTestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        CPTestSuite.class,
        DCDTestFull.class,
        LatticeTestSuite.class,
})
public class DataFlowTestSuite {
}
