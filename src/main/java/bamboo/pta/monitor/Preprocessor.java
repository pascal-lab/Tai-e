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

package bamboo.pta.monitor;

import bamboo.pta.core.ProgramManager;
import bamboo.pta.core.solver.PointerAnalysis;
import bamboo.pta.options.Options;

public class Preprocessor implements AnalysisMonitor {

    private ProgramManager pm;

    @Override
    public void setPointerAnalysis(PointerAnalysis pta) {
        pm = pta.getProgramManager();
    }

    @Override
    public void signalPreprocessing() {
        if (Options.get().isPreBuildIR()) {
            pm.buildIRForAllMethods();
        }
    }
}
