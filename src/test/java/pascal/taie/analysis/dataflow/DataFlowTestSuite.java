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

package pascal.taie.analysis.dataflow;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import pascal.taie.analysis.dataflow.clients.constprop.CPTestSuite;
import pascal.taie.analysis.dataflow.clients.deadcode.DCDTestFull;
import pascal.taie.analysis.dataflow.lattice.LatticeTestSuite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        CPTestSuite.class,
        DCDTestFull.class,
        LatticeTestSuite.class,
})
public class DataFlowTestSuite {
}
