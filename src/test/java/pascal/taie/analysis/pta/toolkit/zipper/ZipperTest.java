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

package pascal.taie.analysis.pta.toolkit.zipper;

import org.junit.Test;
import pascal.taie.World;
import pascal.taie.analysis.Tests;
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.toolkit.PointerAnalysisResultExImpl;
import pascal.taie.util.graph.DotDumper;

import java.util.stream.Stream;

public class ZipperTest {

    private static final String CS = "contextsensitivity";

    private static final String BASIC = "basic";

    private static final String MISC = "misc";

    @Test
    public void testOAG() {
        dumpOAG(CS, "TwoObject", "cs:2-obj");
    }

    private static void dumpOAG(String dir, String main, String opts) {
        Tests.testPTA(false, dir, main, opts);
        PointerAnalysisResult pta = World.get().getResult(PointerAnalysis.ID);
        ObjectAllocationGraph oag = new ObjectAllocationGraph(
                new PointerAnalysisResultExImpl(pta, true));
        new DotDumper<Obj>().dump(oag, World.get().getOptions().getOutputDir()
                + "/" + main + "-oag.dot");
    }

    @Test
    public void testOFG() {
        Stream.of("Cast", "StoreLoad", "Array", "CallParamRet", "Cycle")
                .forEach(main -> dumpOFG(BASIC, main));
    }

    private static void dumpOFG(String dir, String main) {
        Tests.testPTA(false, dir, main);
        PointerAnalysisResult pta = World.get().getResult(PointerAnalysis.ID);
        ObjectFlowGraph ofg = new ObjectFlowGraph(pta);
        FGDumper.dump(ofg, World.get().getOptions().getOutputDir()
                        + "/" + main + "-ofg.dot");
    }

    @Test
    public void testPFGBuilder() {
        Tests.testPTA(false, MISC, "Zipper", "pre:zipper");
    }
}
