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

package pascal.taie;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import pascal.taie.analysis.dataflow.DataflowTestSuite;
import pascal.taie.analysis.graph.callgraph.cha.CHATestFull;
import pascal.taie.analysis.pta.CSPTATest;
import pascal.taie.config.OptionsTest;
import pascal.taie.frontend.soot.SootFrontendTest;
import pascal.taie.language.DefaultMethodTest;
import pascal.taie.language.HierarchyTest;
import pascal.taie.language.TypeTest;
import pascal.taie.util.UtilTestSuite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        // world
        SootFrontendTest.class,
        TypeTest.class,
        HierarchyTest.class,
        DefaultMethodTest.class,
        // analysis
        DataflowTestSuite.class,
        CHATestFull.class,
        CSPTATest.class,
        // util
        OptionsTest.class,
        UtilTestSuite.class,
})
public class TaieTestSuite {
}
