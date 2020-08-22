/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.pta.env.nativemodel;

import bamboo.pta.core.ProgramManager;
import bamboo.pta.core.solver.PointerAnalysis;
import bamboo.pta.element.Method;
import bamboo.pta.monitor.AnalysisMonitor;
import bamboo.pta.statement.Statement;

/**
 * Default modeling of native code.
 * Note that current modeling is not suitable for flow-sensitive analysis.
 *
 * TODO: for correctness, record which methods have been processed?
 */
public class DefaultNativeModel2 implements AnalysisMonitor {

    private MethodModel methodModel;
    private CallModel callModel;
    private FinalizerModel finalizerModel;

    @Override
    public void setPointerAnalysis(PointerAnalysis pta) {
        ProgramManager pm = pta.getProgramManager();
        methodModel = new MethodModel(pm);
        callModel = new CallModel(pm);
        finalizerModel = new FinalizerModel(pm);
    }

    @Override
    public void signalNewMethod(Method method) {
        methodModel.process(method);
        Statement[] statements = method.getStatements()
                .toArray(new Statement[0]);
        for (Statement s : statements) {
            s.accept(callModel);
            s.accept(finalizerModel);
        }
    }
}
