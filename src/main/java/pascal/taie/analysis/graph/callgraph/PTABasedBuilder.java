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
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;

/**
 * Builds call graph based on pointer analysis results.
 * This builder assumes that pointer analysis have been done,
 * and it merely returns the (context-insensitive) call graph
 * obtained from pointer analysis result.
 */
class PTABasedBuilder implements CGBuilder<Invoke, JMethod> {

    @Override
    public CallGraph<Invoke, JMethod> build() {
        PointerAnalysisResult result = World.getResult(PointerAnalysis.ID);
        return result.getCallGraph();
    }
}
