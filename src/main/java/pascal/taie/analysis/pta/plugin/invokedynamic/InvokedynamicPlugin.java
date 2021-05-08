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

package pascal.taie.analysis.pta.plugin.invokedynamic;

import pascal.taie.analysis.pta.core.solver.PointerAnalysis;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.language.classes.JMethod;

public class InvokedynamicPlugin implements Plugin {

    private PointerAnalysis pta;

    /**
     * Lambdas are supposed to be processed by LambdaPlugin.
     */
    private final boolean processLambdas = false;

    @Override
    public void setPointerAnalysis(PointerAnalysis pta) {
        this.pta = pta;
    }

    @Override
    public void handleNewMethod(JMethod method) {

    }
}
