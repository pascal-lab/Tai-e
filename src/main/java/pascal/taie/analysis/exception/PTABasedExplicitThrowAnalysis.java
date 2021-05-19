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

import pascal.taie.analysis.pta.plugin.Exception.ExceptionHandler;
import pascal.taie.ir.IR;
import pascal.taie.language.classes.JMethod;

/**
 * Analyzes explicit exceptions based on pointer analysis.
 */
class PTABasedExplicitThrowAnalysis implements ExplicitThrowAnalysis {

    private ExceptionHandler exceptionHandler;

    @Override
    public void analyze(IR ir, ThrowResult result) {
        throw new UnsupportedOperationException();
    }

    public static void mayThrowExplicitly(IR ir){
        JMethod jMethod=ir.getMethod();

    }
}
