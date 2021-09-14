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

import pascal.taie.analysis.InterproceduralAnalysis;
import pascal.taie.config.AnalysisConfig;

public class CallGraphBuilder extends InterproceduralAnalysis {

    public static final String ID = "cg";

    public CallGraphBuilder(AnalysisConfig config) {
        super(config);
    }

    @Override
    public Object analyze() {
        throw new UnsupportedOperationException();
    }
}
