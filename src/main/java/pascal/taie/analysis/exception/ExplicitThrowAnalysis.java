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

import pascal.taie.ir.IR;

interface ExplicitThrowAnalysis {

    /**
     * Analyze explicit exceptions (of Throw and Invoke) in given ir
     * and store result.
     */
    void analyze(IR ir, ThrowResult result);
}
