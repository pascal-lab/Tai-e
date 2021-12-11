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
import pascal.taie.analysis.dataflow.analysis.DeadCodeTestFull;
import pascal.taie.analysis.dataflow.analysis.LiveVarTestFull;
import pascal.taie.analysis.dataflow.analysis.constprop.CPTestFull;
import pascal.taie.analysis.dataflow.analysis.constprop.InterCPAliasTestFull;
import pascal.taie.analysis.dataflow.analysis.constprop.InterCPTestFull;
import pascal.taie.analysis.graph.callgraph.cha.CHATestFull;
import pascal.taie.analysis.pta.CIPTATestFull;
import pascal.taie.analysis.pta.CSPTATestFull;
import pascal.taie.analysis.pta.TaintTestFull;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        LiveVarTestFull.class,
        CPTestFull.class,
        DeadCodeTestFull.class,
        CHATestFull.class,
        InterCPTestFull.class,
        CIPTATestFull.class,
        CSPTATestFull.class,
        InterCPAliasTestFull.class,
        TaintTestFull.class,
})
public class AssignmentTestSuite {
}
