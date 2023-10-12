/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import pascal.taie.analysis.bugfinder.BugFinderTestSuite;
import pascal.taie.analysis.dataflow.DataflowTestSuite;
import pascal.taie.analysis.deadcode.DeadCodeTestFull;
import pascal.taie.analysis.defuse.DefUseTest;
import pascal.taie.analysis.graph.callgraph.cha.CHATestFull;
import pascal.taie.analysis.pta.PTATestSuite;
import pascal.taie.analysis.sideeffect.SideEffectTest;
import pascal.taie.config.OptionsTest;
import pascal.taie.frontend.cache.SerializationTest;
import pascal.taie.frontend.soot.SootFrontendTest;
import pascal.taie.language.DefaultMethodTest;
import pascal.taie.language.FieldTest;
import pascal.taie.language.HierarchyTest;
import pascal.taie.language.TypeTest;
import pascal.taie.language.classes.StringRepsTest;
import pascal.taie.language.generics.GSignaturesTest;
import pascal.taie.util.UtilTestSuite;

@Suite
@SelectClasses({
        // world
        SootFrontendTest.class,
        TypeTest.class,
        GSignaturesTest.class,
        HierarchyTest.class,
        DefaultMethodTest.class,
        FieldTest.class,
        SerializationTest.class,
        // analysis
        BugFinderTestSuite.class,
        DataflowTestSuite.class,
        DeadCodeTestFull.class,
        DefUseTest.class,
        CHATestFull.class,
        PTATestSuite.class,
        SideEffectTest.class,
        // util
        OptionsTest.class,
        UtilTestSuite.class,
        StringRepsTest.class,
})
public class TaieTestSuite {
}
