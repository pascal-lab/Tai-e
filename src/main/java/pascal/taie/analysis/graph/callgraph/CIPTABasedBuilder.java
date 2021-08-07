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

package pascal.taie.analysis.graph.callgraph;

import pascal.taie.World;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.ci.CIPTA;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;

/**
 * Call graph builder based on context-insensitive pointer analysis (CIPTA).
 *
 * @see pascal.taie.analysis.pta.ci.CIPTA
 */
class CIPTABasedBuilder implements CGBuilder<Invoke, JMethod> {

    @Override
    public CallGraph<Invoke, JMethod> build() {
        PointerAnalysisResult result = World.getResult(CIPTA.ID);
        return result.getCallGraph();
    }
}
