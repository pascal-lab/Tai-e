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

package pascal.taie.analysis.pta.ci;

import pascal.taie.analysis.InterproceduralAnalysis;
import pascal.taie.config.AnalysisConfig;

/**
 * Context-insensitive pointer analysis.
 */
public class CIPTA extends InterproceduralAnalysis {

    public static final String ID = "cipta";

    public CIPTA(AnalysisConfig config) {
        super(config);
    }

    @Override
    public Object analyze() {
        throw new UnsupportedOperationException();
    }
}
