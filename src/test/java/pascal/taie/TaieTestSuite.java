/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import pascal.taie.callgraph.cha.CHATestFull;
import pascal.taie.dataflow.DataFlowTestSuite;
import pascal.taie.dataflow.analysis.constprop.CPTestSuite;
import pascal.taie.dataflow.lattice.LatticeTestSuite;
import pascal.taie.frontend.soot.SootFrontendTest;
import pascal.taie.java.HierarchyTest;
import pascal.taie.java.TypeTest;
import pascal.taie.pta.CSPTATest;
import pascal.taie.pta.PTAOptionsTest;
import pascal.taie.pta.PTATestFull;
import pascal.taie.util.UtilTestSuite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        // world
        SootFrontendTest.class,
        TypeTest.class,
        HierarchyTest.class,
        // analysis
        CPTestSuite.class,
        LatticeTestSuite.class,
        DataFlowTestSuite.class,
        CHATestFull.class,
        PTATestFull.class,
        CSPTATest.class,
        // util
        PTAOptionsTest.class,
        UtilTestSuite.class,
})
public class TaieTestSuite {
}
