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

package pascal.taie.analysis;

import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;

/**
 * Abstract base class for all method analyses, or say, intra-procedural analyses.
 *
 * @param <R> result type
 */
public abstract class MethodAnalysis<R> extends Analysis {

    // private boolean isParallel;

    protected MethodAnalysis(AnalysisConfig config) {
        super(config);
    }

    /**
     * Runs this analysis for the given {@link IR}.
     * The result will be stored in {@link IR}. If the result is not used
     * by following analyses, then this method should return {@code null}.
     *
     * @param ir IR of the method to be analyzed
     * @return the analysis result for given ir.
     */
    public abstract R analyze(IR ir);
}
