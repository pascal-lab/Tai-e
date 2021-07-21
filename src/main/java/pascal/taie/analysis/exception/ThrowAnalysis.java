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

package pascal.taie.analysis.exception;

import pascal.taie.analysis.IntraproceduralAnalysis;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;

public class ThrowAnalysis extends IntraproceduralAnalysis {

    public static final String ID = "throw";

    /**
     * If this field is null, then this analysis ignores implicit exceptions.
     */
    private final ImplicitThrowAnalysis implicitThrowAnalysis;

    private final ExplicitThrowAnalysis explicitThrowAnalysis;

    public ThrowAnalysis(AnalysisConfig config) {
        super(config);
        if ("all".equals(getOptions().getString("exception"))) {
            implicitThrowAnalysis = ImplicitThrowAnalysis.get();
        } else {
            implicitThrowAnalysis = null;
        }
        if ("pta".equals(getOptions().getString("algorithm"))) {
            explicitThrowAnalysis = new PTABasedExplicitThrowAnalysis();
        } else {
            explicitThrowAnalysis = new IntraExplicitThrowAnalysis();
        }
    }

    @Override
    public ThrowResult analyze(IR ir) {
        ThrowResult result = new ThrowResult(
                ir, implicitThrowAnalysis);
        explicitThrowAnalysis.analyze(ir, result);
        return result;
    }
}
