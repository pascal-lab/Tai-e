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

/**
 * Abstract base class for all whole-program analyses.
 *
 * @param <R> result type
 */
public abstract class ProgramAnalysis<R> extends Analysis {

    protected ProgramAnalysis(AnalysisConfig config) {
        super(config);
    }

    /**
     * Runs this analysis for the whole program.
     * If the result is not used by following analyses, then this method
     * should return {@code null}.
     *
     * @return the analysis result for the whole program.
     * The result will be stored in {@link pascal.taie.World}.
     */
    public abstract R analyze();
}
