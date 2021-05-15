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

import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.language.classes.JMethod;

/**
 * Builds call graph based on pointer analysis results.
 * This builder supposes that pointer analysis have been done, and it
 * merely converts context-sensitive call graph into the corresponding
 * context-insensitive call graph (by projecting out the contexts).
 */
class PTABasedBuilder implements CGBuilder<InvokeExp, JMethod> {

    @Override
    public CallGraph<InvokeExp, JMethod> build() {
        throw new UnsupportedOperationException();
    }
}
