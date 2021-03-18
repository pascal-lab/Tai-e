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
import pascal.taie.analysis.callgraph.cha.CHATestFull;
import pascal.taie.analysis.dataflow.DataFlowTestSuite;
import pascal.taie.analysis.dataflow.analysis.constprop.CPTestSuite;
import pascal.taie.analysis.dataflow.lattice.LatticeTestSuite;
import pascal.taie.frontend.soot.SootFrontendTest;
import pascal.taie.language.HierarchyTest;
import pascal.taie.language.TypeTest;
import pascal.taie.analysis.pta.CSPTATest;
import pascal.taie.analysis.pta.PTAOptionsTest;
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
        CSPTATest.class,
        // util
        PTAOptionsTest.class,
        UtilTestSuite.class,
})
public class TaieTestSuite {
}
