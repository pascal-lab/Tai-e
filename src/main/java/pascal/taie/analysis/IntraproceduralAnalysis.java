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

import pascal.taie.ir.IR;

public abstract class IntraproceduralAnalysis extends Analysis {

    // private boolean isParallel;

    /**
     * Run this analysis for the given IR.
     * @param ir IR of the method to be analyzed
     * @return the analysis results
     */
    public abstract Object analyze(IR ir);
}
