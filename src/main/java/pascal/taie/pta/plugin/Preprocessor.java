/*
 * Tai'e - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai'e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.pta.plugin;

import pascal.taie.pta.core.ProgramManager;
import pascal.taie.pta.core.solver.PointerAnalysis;
import pascal.taie.pta.options.Options;

public class Preprocessor implements Plugin {

    private ProgramManager pm;

    @Override
    public void setPointerAnalysis(PointerAnalysis pta) {
        pm = pta.getProgramManager();
    }

    @Override
    public void preprocess() {
        if (Options.get().isPreBuildIR()) {
            pm.buildIRForAllMethods();
        }
    }
}
