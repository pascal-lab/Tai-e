/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.dataflow;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import bamboo.dataflow.analysis.constprop.CPTestSuite;
import bamboo.dataflow.lattice.LatticeTestSuite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        CPTestSuite.class,
        LatticeTestSuite.class
})
public class DataFlowTestSuite {
}
