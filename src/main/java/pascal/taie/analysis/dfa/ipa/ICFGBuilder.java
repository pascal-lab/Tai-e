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

package pascal.taie.analysis.dfa.ipa;

import pascal.taie.analysis.InterproceduralAnalysis;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;

public class ICFGBuilder extends InterproceduralAnalysis {

    public static final String ID = "icfg";

    public ICFGBuilder(AnalysisConfig config) {
        super(config);
    }

    @Override
    public ICFG<JMethod, Stmt> analyze() {
        throw new UnsupportedOperationException();
    }
}
