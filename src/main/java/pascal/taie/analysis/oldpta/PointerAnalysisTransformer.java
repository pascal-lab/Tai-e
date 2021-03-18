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

package pascal.taie.analysis.oldpta;

import pascal.taie.analysis.oldpta.core.solver.PointerAnalysis;
import pascal.taie.analysis.oldpta.core.solver.PointerAnalysisBuilder;
import pascal.taie.frontend.soot.JimplePointerAnalysis;
import pascal.taie.frontend.soot.SootWorldBuilder;
import pascal.taie.analysis.pta.PTAOptions;
import soot.Scene;
import soot.SceneTransformer;

import java.util.Map;

public class PointerAnalysisTransformer extends SceneTransformer {

    private static final PointerAnalysisTransformer INSTANCE =
            new PointerAnalysisTransformer();

    private PointerAnalysisTransformer() {
    }

    public static PointerAnalysisTransformer v() {
        return INSTANCE;
    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        new SootWorldBuilder(Scene.v()).build();

        PointerAnalysis pta = new PointerAnalysisBuilder()
                .build(PTAOptions.get());
        pta.analyze();
        JimplePointerAnalysis.get().setPointerAnalysis(pta);
    }
}
