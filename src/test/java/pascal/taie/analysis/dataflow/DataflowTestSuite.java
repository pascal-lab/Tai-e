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
import pascal.taie.analysis.dataflow.analysis.AvailExpTest;
import pascal.taie.analysis.dataflow.analysis.DeadCodeTestFull;
import pascal.taie.analysis.dataflow.analysis.LiveVarTestFull;
import pascal.taie.analysis.dataflow.analysis.ReachDefTest;
import pascal.taie.analysis.dataflow.analysis.constprop.CPTestSuite;
import pascal.taie.analysis.dataflow.fact.FactTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        FactTest.class,
        CPTestSuite.class,
        DeadCodeTestFull.class,
        LiveVarTestFull.class,
        ReachDefTest.class,
        AvailExpTest.class,
})
public class DataflowTestSuite {
}
