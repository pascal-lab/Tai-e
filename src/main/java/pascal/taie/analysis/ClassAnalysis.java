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
import pascal.taie.language.classes.JClass;

/**
 * Abstract base class for all class analyses, or say, intra-class analyses.
 */
public abstract class ClassAnalysis extends Analysis {

    protected ClassAnalysis(AnalysisConfig config) {
        super(config);
    }

    /**
     * Runs this analysis for the given {@link JClass}.
     * The result will be stored in {@link JClass}. If the result is not used
     * by following analyses, then this method should return {@code null}.
     *
     * @param jclass the class to be analyzed
     * @return the analysis result for given class.
     */
    public abstract Object analyze(JClass jclass);
}
