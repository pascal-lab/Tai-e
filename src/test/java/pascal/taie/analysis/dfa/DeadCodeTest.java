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

package pascal.taie.analysis.dfa;

import org.junit.Test;
import pascal.taie.Main;

import java.util.List;

public class DeadCodeTest {

    private static final String CP = "test-resources/dataflow/deadcode/";

    @Test
    public void runTests() {
        List.of(
                "ControlFlowUnreachable",
                "ControlFlowUnreachable2", // may trigger NPE before
                "DeadAssignment",
                 "Loops",
                "MixedDeadCode", // may trigger NPE before
                "NotDead",
                "UnreachableBranch",
                "UnreachableBranch2"
        ).forEach(DeadCodeTest::test);
    }

    private static void test(String main) {
        Main.main(new String[]{"-pp", "-a", "deadcode", "-cp", CP, "-m", main});
    }
}
