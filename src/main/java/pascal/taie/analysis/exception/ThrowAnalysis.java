/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.analysis.exception;

import pascal.taie.analysis.IntraproceduralAnalysis;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;

import javax.annotation.Nullable;

public class ThrowAnalysis extends IntraproceduralAnalysis {

    public static final String ID = "throw";

    /**
     * If this field is null, then this analysis ignores implicit exceptions.
     */
    @Nullable
    private final ImplicitThrowAnalysis implicitThrowAnalysis;

    private final ExplicitThrowAnalysis explicitThrowAnalysis;

    public ThrowAnalysis(AnalysisConfig config) {
        super(config);
        if ("all".equals(getOptions().getString("exception"))) {
            implicitThrowAnalysis = ImplicitThrowAnalysis.get();
        } else {
            implicitThrowAnalysis = null;
        }
        if ("inter".equals(getOptions().getString("scope"))) {
            explicitThrowAnalysis = new InterExplicitThrowAnalysis();
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
