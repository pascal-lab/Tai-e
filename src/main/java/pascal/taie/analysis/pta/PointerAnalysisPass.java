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

package pascal.taie.analysis.pta;

import pascal.taie.World;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.core.solver.PointerAnalysisBuilder;
import pascal.taie.pass.Pass;

public class PointerAnalysisPass implements Pass {

    @Override
    public void run() {
        Solver solver = new PointerAnalysisBuilder()
                .build(World.getOptions());
        solver.solve();
    }
}
